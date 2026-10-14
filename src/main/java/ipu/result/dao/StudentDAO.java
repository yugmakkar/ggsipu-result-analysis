package ipu.result.dao;

import ipu.result.model.College;
import ipu.result.model.Programme;
import ipu.result.model.Student;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Reference data for colleges and programmes, and the student table.
 * All methods use the caller's connection so they can share a transaction.
 * Every statement is parameterised (no string-built SQL).
 */
public class StudentDAO {

    public void upsertCollege(Connection c, College col) throws SQLException {
        if (!update(c, "UPDATE college SET college_name=? WHERE college_code=?", col.name(), col.code())) {
            exec(c, "INSERT INTO college (college_code, college_name) VALUES (?,?)", col.code(), col.name());
        }
    }

    public void upsertProgramme(Connection c, Programme p) throws SQLException {
        if (!update(c, "UPDATE programme SET programme_name=?, duration_years=? WHERE programme_code=?",
                p.name(), p.durationYears(), p.code())) {
            exec(c, "INSERT INTO programme (programme_code, programme_name, duration_years) VALUES (?,?,?)",
                    p.code(), p.name(), p.durationYears());
        }
    }

    public void upsertStudent(Connection c, Student s) throws SQLException {
        if (!update(c, "UPDATE student SET name=?, college_code=?, programme_code=?, batch_year=? WHERE enrollment_no=?",
                s.name(), s.collegeCode(), s.programmeCode(), s.batchYear(), s.enrollmentNo())) {
            exec(c, "INSERT INTO student (enrollment_no, name, college_code, programme_code, batch_year) VALUES (?,?,?,?,?)",
                    s.enrollmentNo(), s.name(), s.collegeCode(), s.programmeCode(), s.batchYear());
        }
    }

    public Optional<Student> find(Connection c, String enrollmentNo) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT enrollment_no, name, college_code, programme_code, batch_year FROM student WHERE enrollment_no=?")) {
            ps.setString(1, enrollmentNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Integer batch = rs.getObject("batch_year") == null ? null : rs.getInt("batch_year");
                return Optional.of(new Student(rs.getString("enrollment_no"), rs.getString("name"),
                        rs.getString("college_code"), rs.getString("programme_code"), batch));
            }
        }
    }

    public String collegeName(Connection c, String code) throws SQLException {
        return scalar(c, "SELECT college_name FROM college WHERE college_code=?", code);
    }

    public String programmeName(Connection c, String code) throws SQLException {
        return scalar(c, "SELECT programme_name FROM programme WHERE programme_code=?", code);
    }

    // --- small JDBC helpers ---

    static boolean update(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, args);
            return ps.executeUpdate() > 0;
        }
    }

    static void exec(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, args);
            ps.executeUpdate();
        }
    }

    static String scalar(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, args);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    static void bind(PreparedStatement ps, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
    }
}
