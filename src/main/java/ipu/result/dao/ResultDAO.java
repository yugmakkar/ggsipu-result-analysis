package ipu.result.dao;

import ipu.result.dto.Marksheet;
import ipu.result.dto.StudentRow;
import ipu.result.dto.Toppers;
import ipu.result.model.College;
import ipu.result.model.Programme;
import ipu.result.model.SemesterResult;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static ipu.result.dao.StudentDAO.exec;
import static ipu.result.dao.StudentDAO.update;

/** Semester results, CGPA lookups, the marksheet, and the four topper queries. */
public class ResultDAO {

    public void upsertSemesterResult(Connection c, SemesterResult r) throws SQLException {
        if (!update(c, "UPDATE semester_result SET total_credits=?, credit_points=?, sgpa=?, has_backlog=? " +
                        "WHERE enrollment_no=? AND semester=?",
                r.totalCredits(), r.creditPoints(), r.sgpa(), r.hasBacklog(), r.enrollmentNo(), r.semester())) {
            exec(c, "INSERT INTO semester_result (enrollment_no, semester, total_credits, credit_points, sgpa, has_backlog) " +
                            "VALUES (?,?,?,?,?,?)",
                    r.enrollmentNo(), r.semester(), r.totalCredits(), r.creditPoints(), r.sgpa(), r.hasBacklog());
        }
    }

    // ---- counts for the dashboard ----

    public int count(Connection c, String table) throws SQLException {
        // table is a fixed internal constant, never user input.
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM " + table);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public record UploadRow(int id, String fileName, String session, String uploadedOn, int rowCount) {
        public int getId() { return id; }
        public String getFileName() { return fileName; }
        public String getSession() { return session; }
        public String getUploadedOn() { return uploadedOn; }
        public int getRowCount() { return rowCount; }
    }

    public List<UploadRow> recentUploads(Connection c, int limit) throws SQLException {
        List<UploadRow> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT upload_id, file_name, exam_session, uploaded_on, row_count " +
                "FROM result_upload ORDER BY uploaded_on DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new UploadRow(rs.getInt(1), rs.getString(2), rs.getString(3),
                            String.valueOf(rs.getTimestamp(4)), rs.getInt(5)));
                }
            }
        }
        return out;
    }

    // ---- admin: list all students, optionally filtered ----

    /** All students (admin view), optionally filtered by college and/or programme. */
    public List<StudentRow> listStudents(Connection c, String collegeCode, String programmeCode)
            throws SQLException {
        String col = (collegeCode == null || collegeCode.isBlank()) ? null : collegeCode;
        String prog = (programmeCode == null || programmeCode.isBlank()) ? null : programmeCode;
        String sql =
                "SELECT s.enrollment_no, s.name, s.college_code, s.programme_code, " +
                "       COALESCE(v.cgpa, 0), COALESCE(v.has_backlog, 0) " +
                "FROM student s LEFT JOIN v_cgpa v ON v.enrollment_no = s.enrollment_no " +
                "WHERE (? IS NULL OR s.college_code = ?) " +
                "  AND (? IS NULL OR s.programme_code = ?) " +
                "ORDER BY s.college_code, s.programme_code, s.name";
        List<StudentRow> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, col);  ps.setString(2, col);
            ps.setString(3, prog); ps.setString(4, prog);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new StudentRow(rs.getString(1), rs.getString(2), rs.getString(3),
                            rs.getString(4), rs.getDouble(5), rs.getInt(6) != 0));
                }
            }
        }
        return out;
    }

    public List<College> listColleges(Connection c) throws SQLException {
        List<College> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT college_code, college_name FROM college ORDER BY college_code");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(new College(rs.getString(1), rs.getString(2)));
        }
        return out;
    }

    public List<Programme> listProgrammes(Connection c) throws SQLException {
        List<Programme> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT programme_code, programme_name, duration_years FROM programme ORDER BY programme_code");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(new Programme(rs.getString(1), rs.getString(2), rs.getInt(3)));
        }
        return out;
    }

    // ---- student marksheet ----

    public Marksheet marksheet(Connection c, String enrollmentNo) throws SQLException {
        String name, collegeCode, collegeName, progCode, progName;
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT s.name, s.college_code, co.college_name, s.programme_code, pr.programme_name " +
                "FROM student s " +
                "LEFT JOIN college co ON co.college_code = s.college_code " +
                "LEFT JOIN programme pr ON pr.programme_code = s.programme_code " +
                "WHERE s.enrollment_no=?")) {
            ps.setString(1, enrollmentNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                name = rs.getString(1);
                collegeCode = rs.getString(2);
                collegeName = rs.getString(3);
                progCode = rs.getString(4);
                progName = rs.getString(5);
            }
        }

        // SGPA/CGPA only exist once end-semester results are imported.
        double cgpa = 0.0;
        boolean cgpaBacklog = false;
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT cgpa, has_backlog FROM v_cgpa WHERE enrollment_no=?")) {
            ps.setString(1, enrollmentNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    cgpa = rs.getDouble(1);
                    cgpaBacklog = rs.getInt(2) != 0;
                }
            }
        }

        // SGPA per semester, if computed.
        Map<Integer, double[]> sgpaBySem = new LinkedHashMap<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT semester, sgpa, has_backlog FROM semester_result WHERE enrollment_no=?")) {
            ps.setString(1, enrollmentNo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) sgpaBySem.put(rs.getInt(1), new double[]{rs.getDouble(2), rs.getBoolean(3) ? 1 : 0});
            }
        }

        // Semesters are derived from the marks the student actually has, so an
        // internal-only student (no semester_result row) still shows a marksheet.
        List<Marksheet.SemesterBlock> blocks = new ArrayList<>();
        boolean anyExternal = false;
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT DISTINCT su.semester FROM marks m JOIN subject su ON su.paper_id = m.paper_id " +
                "WHERE m.enrollment_no=? ORDER BY su.semester")) {
            ps.setString(1, enrollmentNo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int sem = rs.getInt(1);
                    List<Marksheet.MarkRow> rows = marksForSemester(c, enrollmentNo, sem);
                    for (Marksheet.MarkRow r : rows) {
                        if (r.external() != null || r.total() != null) anyExternal = true;
                    }
                    double[] sg = sgpaBySem.get(sem);
                    blocks.add(new Marksheet.SemesterBlock(
                            sem, sg == null ? 0.0 : sg[0], sg != null && sg[1] != 0, rows));
                }
            }
        }
        boolean internalOnly = !anyExternal;
        return new Marksheet(enrollmentNo, name, collegeCode, collegeName, progCode, progName,
                cgpa, cgpaBacklog, internalOnly, blocks);
    }

    private List<Marksheet.MarkRow> marksForSemester(Connection c, String enrollmentNo, int semester) throws SQLException {
        List<Marksheet.MarkRow> rows = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT su.paper_id, su.paper_code, su.subject_name, su.credits, su.internal_max, " +
                "m.internal_marks, m.external_marks, m.total_marks, m.grade, m.absent " +
                "FROM marks m JOIN subject su ON su.paper_id = m.paper_id " +
                "WHERE m.enrollment_no=? AND su.semester=? ORDER BY su.paper_id")) {
            ps.setString(1, enrollmentNo);
            ps.setInt(2, semester);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Marksheet.MarkRow(
                            rs.getString(1), rs.getString(2), rs.getString(3), rs.getInt(4), intOrNull(rs, 5),
                            intOrNull(rs, 6), intOrNull(rs, 7), intOrNull(rs, 8),
                            rs.getString(9), rs.getBoolean(10)));
                }
            }
        }
        return rows;
    }

    // ---- toppers ----

    /** Subject-wise: top scorer(s) per paper (ties kept). Absent marks excluded. */
    public List<Toppers.SubjectTopper> subjectToppers(Connection c) throws SQLException {
        String sql =
                "SELECT paper_id, subject_name, enrollment_no, name, college_code, total_marks, grade FROM (" +
                "  SELECT m.paper_id, su.subject_name, m.enrollment_no, st.name, st.college_code, " +
                "         m.total_marks, m.grade, " +
                "         RANK() OVER (PARTITION BY m.paper_id ORDER BY m.total_marks DESC) AS rnk " +
                "  FROM marks m " +
                "  JOIN subject su ON su.paper_id = m.paper_id " +
                "  JOIN student st ON st.enrollment_no = m.enrollment_no " +
                "  WHERE m.absent = FALSE AND m.total_marks IS NOT NULL" +
                ") t WHERE rnk = 1 ORDER BY paper_id, name";
        List<Toppers.SubjectTopper> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new Toppers.SubjectTopper(rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getInt(6), rs.getString(7)));
            }
        }
        return out;
    }

    /** College-wise: highest CGPA within each college, among students with no backlog. */
    public List<Toppers.RankTopper> collegeToppers(Connection c) throws SQLException {
        String sql =
                "SELECT college_code, enrollment_no, name, programme_code, cgpa FROM (" +
                "  SELECT s.college_code, v.enrollment_no, s.name, s.programme_code, v.cgpa, " +
                "         RANK() OVER (PARTITION BY s.college_code ORDER BY v.cgpa DESC) AS rnk " +
                "  FROM v_cgpa v JOIN student s ON s.enrollment_no = v.enrollment_no " +
                "  WHERE v.has_backlog = 0" +
                ") t WHERE rnk = 1 ORDER BY college_code, name";
        return rankRows(c, sql, "college", 1, 2, 3, 4, 5);
    }

    /** Semester-wise: highest SGPA per semester, among students with no backlog that semester. */
    public List<Toppers.RankTopper> semesterToppers(Connection c) throws SQLException {
        String sql =
                "SELECT semester, enrollment_no, name, college_code, programme_code, sgpa FROM (" +
                "  SELECT r.semester, r.enrollment_no, s.name, s.college_code, s.programme_code, r.sgpa, " +
                "         RANK() OVER (PARTITION BY r.semester ORDER BY r.sgpa DESC) AS rnk " +
                "  FROM semester_result r JOIN student s ON s.enrollment_no = r.enrollment_no " +
                "  WHERE r.has_backlog = FALSE" +
                ") t WHERE rnk = 1 ORDER BY semester, name";
        List<Toppers.RankTopper> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new Toppers.RankTopper("Semester " + rs.getInt(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getDouble(6)));
            }
        }
        return out;
    }

    /** Overall: highest CGPA across the whole university, among students with no backlog. */
    public List<Toppers.RankTopper> overallToppers(Connection c) throws SQLException {
        String sql =
                "SELECT enrollment_no, name, college_code, programme_code, cgpa FROM (" +
                "  SELECT v.enrollment_no, s.name, s.college_code, s.programme_code, v.cgpa, " +
                "         RANK() OVER (ORDER BY v.cgpa DESC) AS rnk " +
                "  FROM v_cgpa v JOIN student s ON s.enrollment_no = v.enrollment_no " +
                "  WHERE v.has_backlog = 0" +
                ") t WHERE rnk = 1 ORDER BY name";
        List<Toppers.RankTopper> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new Toppers.RankTopper("University", rs.getString(1), rs.getString(2),
                        rs.getString(3), rs.getString(4), rs.getDouble(5)));
            }
        }
        return out;
    }

    private List<Toppers.RankTopper> rankRows(Connection c, String sql, String scopePrefix,
                                              int scopeCol, int enrollCol, int nameCol,
                                              int progCol, int valCol) throws SQLException {
        List<Toppers.RankTopper> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new Toppers.RankTopper(scopePrefix + " " + rs.getString(scopeCol),
                        rs.getString(enrollCol), rs.getString(nameCol),
                        rs.getString(scopeCol), rs.getString(progCol), rs.getDouble(valCol)));
            }
        }
        return out;
    }

    private static Integer intOrNull(ResultSet rs, int col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }
}
