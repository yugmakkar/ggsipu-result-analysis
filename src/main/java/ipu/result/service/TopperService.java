package ipu.result.service;

import ipu.result.dao.Db;
import ipu.result.dao.ResultDAO;
import ipu.result.dto.Marksheet;
import ipu.result.dto.StudentRow;
import ipu.result.dto.Toppers;
import ipu.result.model.College;
import ipu.result.model.Programme;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** Read-side façade over {@link ResultDAO} for the UI layers. */
public class TopperService {

    private final Db db;
    private final ResultDAO resultDAO = new ResultDAO();

    public TopperService(Db db) { this.db = db; }

    public List<Toppers.SubjectTopper> subjectToppers()  { return read(resultDAO::subjectToppers); }
    public List<Toppers.RankTopper>    collegeToppers()  { return read(resultDAO::collegeToppers); }
    public List<Toppers.RankTopper>    semesterToppers() { return read(resultDAO::semesterToppers); }
    public List<Toppers.RankTopper>    overallToppers()  { return read(resultDAO::overallToppers); }

    public List<StudentRow> listStudents(String collegeCode, String programmeCode) {
        try (Connection c = db.open()) {
            return resultDAO.listStudents(c, collegeCode, programmeCode);
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public List<College> listColleges() {
        try (Connection c = db.open()) { return resultDAO.listColleges(c); }
        catch (SQLException e) { throw new RuntimeException(e); }
    }

    public List<Programme> listProgrammes() {
        try (Connection c = db.open()) { return resultDAO.listProgrammes(c); }
        catch (SQLException e) { throw new RuntimeException(e); }
    }

    public Marksheet marksheet(String enrollmentNo) {
        try (Connection c = db.open()) {
            return resultDAO.marksheet(c, enrollmentNo);
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public List<ResultDAO.UploadRow> recentUploads(int limit) {
        try (Connection c = db.open()) {
            return resultDAO.recentUploads(c, limit);
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public int studentCount() { return count("student"); }
    public int collegeCount() { return count("college"); }
    public int uploadCount()  { return count("result_upload"); }

    private int count(String table) {
        try (Connection c = db.open()) {
            return resultDAO.count(c, table);
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private interface Query<T> { List<T> run(Connection c) throws SQLException; }

    private <T> List<T> read(Query<T> q) {
        try (Connection c = db.open()) {
            return q.run(c);
        } catch (SQLException e) { throw new RuntimeException(e); }
    }
}
