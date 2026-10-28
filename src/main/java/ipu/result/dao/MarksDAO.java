package ipu.result.dao;

import ipu.result.model.Mark;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

/** Insert/replace of marks. Re-importing a semester replaces that semester's rows. */
public class MarksDAO {

    /** Deletes a student's marks for all papers in the given semester (idempotent re-import). */
    public void deleteForStudentSemester(Connection c, String enrollmentNo, int semester) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "DELETE FROM marks WHERE enrollment_no=? AND paper_id IN " +
                "(SELECT paper_id FROM subject WHERE semester=?)")) {
            ps.setString(1, enrollmentNo);
            ps.setInt(2, semester);
            ps.executeUpdate();
        }
    }

    public void insert(Connection c, Mark m, int uploadId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO marks (enrollment_no, paper_id, upload_id, internal_marks, external_marks, " +
                "total_marks, grade, absent) VALUES (?,?,?,?,?,?,?,?)")) {
            ps.setString(1, m.enrollmentNo());
            ps.setString(2, m.paperId());
            ps.setInt(3, uploadId);
            setIntOrNull(ps, 4, m.internal());
            setIntOrNull(ps, 5, m.external());
            setIntOrNull(ps, 6, m.total());
            ps.setString(7, m.grade());
            ps.setBoolean(8, m.absent());
            ps.executeUpdate();
        }
    }

    public void insertAll(Connection c, List<Mark> marks, int uploadId) throws SQLException {
        for (Mark m : marks) insert(c, m, uploadId);
    }

    /**
     * Upserts a full end-semester mark keyed on (enrollment, paper): internal,
     * external, total, grade and the absent flag are all written. Unlike a
     * delete-then-insert of the whole semester, this leaves a student's other
     * papers untouched &mdash; essential for a "revised" gazette that re-states
     * only one or two papers.
     */
    public void upsertEndSem(Connection c, Mark m, int uploadId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE marks SET upload_id=?, internal_marks=?, external_marks=?, " +
                "total_marks=?, grade=?, absent=? WHERE enrollment_no=? AND paper_id=?")) {
            ps.setInt(1, uploadId);
            setIntOrNull(ps, 2, m.internal());
            setIntOrNull(ps, 3, m.external());
            setIntOrNull(ps, 4, m.total());
            ps.setString(5, m.grade());
            ps.setBoolean(6, m.absent());
            ps.setString(7, m.enrollmentNo());
            ps.setString(8, m.paperId());
            if (ps.executeUpdate() > 0) return;
        }
        insert(c, m, uploadId);
    }

    /**
     * All of a student's marks in one semester that carry an end-semester result
     * (a grade, or an absence). Internal-only rows are skipped, so SGPA is
     * recomputed from exactly the papers that have been declared.
     */
    public List<Mark> gradedMarksForSemester(Connection c, String enrollmentNo, int semester)
            throws SQLException {
        List<Mark> out = new java.util.ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT m.paper_id, m.internal_marks, m.external_marks, m.total_marks, m.grade, m.absent " +
                "FROM marks m JOIN subject su ON su.paper_id = m.paper_id " +
                "WHERE m.enrollment_no=? AND su.semester=? AND (m.grade IS NOT NULL OR m.absent = TRUE)")) {
            ps.setString(1, enrollmentNo);
            ps.setInt(2, semester);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Mark(enrollmentNo, rs.getString(1),
                            intOrNull(rs, 2), intOrNull(rs, 3), intOrNull(rs, 4),
                            rs.getString(5), rs.getBoolean(6)));
                }
            }
        }
        return out;
    }

    private static Integer intOrNull(java.sql.ResultSet rs, int col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }

    /**
     * Upserts an internal-assessment mark keyed on (enrollment, paper). Only the
     * internal column, upload id and absent flag are written; any existing
     * external/total/grade (from an end-semester import) are left untouched, and
     * a re-upload of the same sheet updates in place rather than duplicating.
     */
    public void upsertInternal(Connection c, Mark m, int uploadId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE marks SET internal_marks=?, upload_id=?, absent=? WHERE enrollment_no=? AND paper_id=?")) {
            setIntOrNull(ps, 1, m.internal());
            ps.setInt(2, uploadId);
            ps.setBoolean(3, m.absent());
            ps.setString(4, m.enrollmentNo());
            ps.setString(5, m.paperId());
            if (ps.executeUpdate() > 0) return;
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO marks (enrollment_no, paper_id, upload_id, internal_marks, absent) VALUES (?,?,?,?,?)")) {
            ps.setString(1, m.enrollmentNo());
            ps.setString(2, m.paperId());
            ps.setInt(3, uploadId);
            setIntOrNull(ps, 4, m.internal());
            ps.setBoolean(5, m.absent());
            ps.executeUpdate();
        }
    }

    private static void setIntOrNull(PreparedStatement ps, int idx, Integer v) throws SQLException {
        if (v == null) ps.setNull(idx, Types.INTEGER);
        else ps.setInt(idx, v);
    }
}
