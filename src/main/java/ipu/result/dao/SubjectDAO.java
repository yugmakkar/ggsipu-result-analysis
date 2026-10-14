package ipu.result.dao;

import ipu.result.model.Subject;
import ipu.result.service.GradeScale;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ipu.result.dao.StudentDAO.exec;
import static ipu.result.dao.StudentDAO.update;

/** Subjects (papers) and the grade scale lookup. */
public class SubjectDAO {

    public void upsert(Connection c, Subject s) throws SQLException {
        if (!update(c, "UPDATE subject SET paper_code=?, subject_name=?, credits=?, semester=?, programme_code=? WHERE paper_id=?",
                s.paperCode(), s.name(), s.credits(), s.semester(), s.programmeCode(), s.paperId())) {
            exec(c, "INSERT INTO subject (paper_id, paper_code, subject_name, credits, semester, programme_code) VALUES (?,?,?,?,?,?)",
                    s.paperId(), s.paperCode(), s.name(), s.credits(), s.semester(), s.programmeCode());
        }
    }

    /** paper_id -> credits, for every known subject. */
    public Map<String, Integer> creditsByPaper(Connection c) throws SQLException {
        Map<String, Integer> out = new HashMap<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT paper_id, credits FROM subject");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.put(rs.getString(1), rs.getInt(2));
        }
        return out;
    }

    /** paper_id -> semester. */
    public Map<String, Integer> semesterByPaper(Connection c) throws SQLException {
        Map<String, Integer> out = new HashMap<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT paper_id, semester FROM subject");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.put(rs.getString(1), rs.getInt(2));
        }
        return out;
    }

    /** Loads the grade scale from the DB; falls back to the default if empty. */
    public GradeScale loadGradeScale(Connection c) throws SQLException {
        List<GradeScale.Band> bands = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT grade, min_marks, max_marks, grade_point FROM grade_scale ORDER BY min_marks DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                bands.add(new GradeScale.Band(rs.getString(1), rs.getInt(2), rs.getInt(3), rs.getInt(4)));
            }
        }
        return bands.isEmpty() ? GradeScale.defaultScale() : new GradeScale(bands);
    }
}
