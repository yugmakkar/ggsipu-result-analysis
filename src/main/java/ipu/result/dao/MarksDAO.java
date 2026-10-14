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

    private static void setIntOrNull(PreparedStatement ps, int idx, Integer v) throws SQLException {
        if (v == null) ps.setNull(idx, Types.INTEGER);
        else ps.setInt(idx, v);
    }
}
