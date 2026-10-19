package ipu.result.service;

import ipu.result.dao.*;
import ipu.result.model.*;
import ipu.result.parser.InternalAssessmentExtractor;
import ipu.result.parser.ParsedResult;
import ipu.result.parser.PdfReader;
import ipu.result.parser.ResultExtractor;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Imports one result PDF into the database in a single transaction:
 * parse -> grade -> persist reference data, marks and per-semester results.
 * The whole import commits or rolls back as a unit, and re-importing the same
 * data replaces each affected (student, semester) rather than duplicating it.
 */
public class ImportService {

    private final Db db;
    private final StudentDAO studentDAO = new StudentDAO();
    private final SubjectDAO subjectDAO = new SubjectDAO();
    private final MarksDAO marksDAO = new MarksDAO();
    private final ResultDAO resultDAO = new ResultDAO();

    public ImportService(Db db) { this.db = db; }

    public record ImportSummary(int uploadId, int students, int marks, int semesterResults, String session) {}

    /**
     * Reads, parses and imports a PDF file. {@code storedName} is the on-disk name.
     *
     * The format is detected from the text: a GGSIPU <i>Internal Assessment</i>
     * sheet is parsed and stored as internal marks (the Semester-1 path); any
     * other PDF falls through to the end-semester grammar.
     */
    public ImportSummary importPdf(File pdf, String originalName, String storedName) throws IOException {
        String text = new PdfReader().readText(pdf);
        if (text.contains(InternalAssessmentExtractor.MARKER)) {
            ParsedResult parsed = new InternalAssessmentExtractor().extract(text);
            return persistInternal(parsed, originalName, storedName);
        }
        ParsedResult parsed = new ResultExtractor().extract(text);
        return persist(parsed, originalName, storedName);
    }

    /**
     * Stores internal-assessment marks: reference data, students and one
     * internal mark per (student, paper). No grades or SGPA are computed — those
     * belong to the end-semester result. Runs as a single transaction; a
     * re-upload of the same sheet updates rows in place.
     */
    ImportSummary persistInternal(ParsedResult parsed, String originalName, String storedName) {
        if (parsed.students.isEmpty() || parsed.marks.isEmpty()) {
            throw new IllegalArgumentException(
                    "No internal-assessment rows recognised in the PDF. Expected GGSIPU "
                    + "internal marks sheets (see docs/pdf-format.md).");
        }
        Connection c = null;
        try {
            c = db.open();
            c.setAutoCommit(false);

            int uploadId = insertUpload(c, originalName, storedName, parsed.examSession);

            for (College col : parsed.colleges.values())  studentDAO.upsertCollege(c, col);
            for (Programme p : parsed.programmes.values()) studentDAO.upsertProgramme(c, p);
            for (Subject s : parsed.subjects.values())     subjectDAO.upsert(c, s);

            Map<String, Student> uniqueStudents = new LinkedHashMap<>();
            for (Student s : parsed.students) uniqueStudents.put(s.enrollmentNo(), s);
            for (Student s : uniqueStudents.values())      studentDAO.upsertStudent(c, s);

            int markCount = 0;
            for (Mark m : parsed.marks) {
                marksDAO.upsertInternal(c, m, uploadId);
                markCount++;
            }

            updateUploadRowCount(c, uploadId, markCount);
            c.commit();
            return new ImportSummary(uploadId, uniqueStudents.size(), markCount, 0, parsed.examSession);

        } catch (Exception e) {
            rollbackQuietly(c);
            throw new RuntimeException("import failed and was rolled back: " + e.getMessage(), e);
        } finally {
            closeQuietly(c);
        }
    }

    ImportSummary persist(ParsedResult parsed, String originalName, String storedName) {
        // Guard against a PDF whose layout the parser does not recognise: it would
        // otherwise commit an empty upload and report "0 students". Fail loudly so
        // the caller rolls back and deletes the stored file.
        if (parsed.students.isEmpty() || parsed.marks.isEmpty()) {
            throw new IllegalArgumentException(
                    "No results recognised in the PDF. The parser expects the format "
                    + "documented in docs/pdf-format.md; a real GGSIPU gazette needs "
                    + "ResultExtractor re-tuned to its layout.");
        }
        Connection c = null;
        try {
            c = db.open();
            c.setAutoCommit(false);

            int uploadId = insertUpload(c, originalName, storedName, parsed.examSession);

            for (College col : parsed.colleges.values())   studentDAO.upsertCollege(c, col);
            for (Programme p : parsed.programmes.values())  studentDAO.upsertProgramme(c, p);
            for (Subject s : parsed.subjects.values())      subjectDAO.upsert(c, s);
            // de-duplicate students (they appear once per semester block)
            Map<String, Student> uniqueStudents = new LinkedHashMap<>();
            for (Student s : parsed.students) uniqueStudents.put(s.enrollmentNo(), s);
            for (Student s : uniqueStudents.values())       studentDAO.upsertStudent(c, s);

            GradeScale scale = subjectDAO.loadGradeScale(c);
            GradeCalculator grader = new GradeCalculator(scale);
            GpaCalculator gpa = new GpaCalculator(scale);
            Map<String, Integer> creditsByPaper = subjectDAO.creditsByPaper(c);
            Map<String, Integer> semesterByPaper = subjectDAO.semesterByPaper(c);

            // group marks by student, then by semester
            Map<String, Map<Integer, List<Mark>>> byStudentSem = new LinkedHashMap<>();
            for (Mark m : parsed.marks) {
                Integer sem = semesterByPaper.get(m.paperId());
                if (sem == null) throw new IllegalStateException("mark for unknown paper " + m.paperId());
                Mark graded = grader.assign(m);
                byStudentSem
                        .computeIfAbsent(graded.enrollmentNo(), k -> new LinkedHashMap<>())
                        .computeIfAbsent(sem, k -> new ArrayList<>())
                        .add(graded);
            }

            int markCount = 0, semCount = 0;
            for (var studentEntry : byStudentSem.entrySet()) {
                String enroll = studentEntry.getKey();
                for (var semEntry : studentEntry.getValue().entrySet()) {
                    int semester = semEntry.getKey();
                    List<Mark> marks = semEntry.getValue();

                    marksDAO.deleteForStudentSemester(c, enroll, semester);
                    marksDAO.insertAll(c, marks, uploadId);
                    markCount += marks.size();

                    SemesterResult sr = gpa.semester(enroll, semester, marks, creditsByPaper);
                    resultDAO.upsertSemesterResult(c, sr);
                    semCount++;
                }
            }

            updateUploadRowCount(c, uploadId, markCount);
            c.commit();
            return new ImportSummary(uploadId, uniqueStudents.size(), markCount, semCount, parsed.examSession);

        } catch (Exception e) {
            rollbackQuietly(c);
            throw new RuntimeException("import failed and was rolled back: " + e.getMessage(), e);
        } finally {
            closeQuietly(c);
        }
    }

    private int insertUpload(Connection c, String originalName, String storedName, String session) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO result_upload (file_name, stored_name, exam_session) VALUES (?,?,?)",
                PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, originalName);
            ps.setString(2, storedName);
            ps.setString(3, session);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private void updateUploadRowCount(Connection c, int uploadId, int rows) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE result_upload SET row_count=? WHERE upload_id=?")) {
            ps.setInt(1, rows);
            ps.setInt(2, uploadId);
            ps.executeUpdate();
        }
    }

    private static void rollbackQuietly(Connection c) {
        if (c != null) try { c.rollback(); } catch (SQLException ignore) {}
    }

    private static void closeQuietly(Connection c) {
        if (c != null) try { c.close(); } catch (SQLException ignore) {}
    }
}
