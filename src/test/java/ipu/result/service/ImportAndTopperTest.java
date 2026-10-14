package ipu.result.service;

import ipu.result.dao.Db;
import ipu.result.dao.ResultDAO;
import ipu.result.dto.Marksheet;
import ipu.result.dto.Toppers;
import ipu.result.parser.FixturePdfGenerator;
import ipu.result.parser.ParsedResult;
import ipu.result.parser.ResultExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ImportAndTopperTest {

    private Db db;
    private TopperService toppers;

    @BeforeEach
    void setUp() {
        // unique in-memory DB per test
        String url = "jdbc:h2:mem:t" + System.nanoTime()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        db = new Db(url, "sa", "");
        db.initSchema();
        toppers = new TopperService(db);
    }

    private ImportService.ImportSummary importFixture() {
        ParsedResult parsed = new ResultExtractor()
                .extract(String.join("\n", FixturePdfGenerator.buildScript()));
        return new ImportService(db).persist(parsed, "sample-result.pdf", "stored.pdf");
    }

    @Test
    void importPersistsEverything() {
        ImportService.ImportSummary s = importFixture();
        assertEquals(8, s.students());
        assertEquals(48, s.marks());
        assertEquals(16, s.semesterResults()); // 8 students x 2 semesters
        assertEquals(8, toppers.studentCount());
        assertEquals(2, toppers.collegeCount());
    }

    @Test
    void reimportIsIdempotent() throws Exception {
        importFixture();
        importFixture(); // same data again
        assertEquals(8, toppers.studentCount());
        try (Connection c = db.open()) {
            assertEquals(48, new ResultDAO().count(c, "marks"));
            assertEquals(16, new ResultDAO().count(c, "semester_result"));
        }
    }

    @Test
    void overallTopperIsMeeraAndExcludesBacklog() {
        importFixture();
        List<Toppers.RankTopper> overall = toppers.overallToppers();
        assertEquals(1, overall.size());
        assertEquals("00401012024", overall.get(0).enrollmentNo());
        assertEquals(9.86, overall.get(0).value(), 0.001);
    }

    @Test
    void collegeToppersOnePerCollege() {
        importFixture();
        List<Toppers.RankTopper> list = toppers.collegeToppers();
        assertEquals(2, list.size());
        // USICT (101) -> Meera ; MAIT (164) -> Ananya
        assertTrue(list.stream().anyMatch(t -> t.collegeCode().equals("101") && t.enrollmentNo().equals("00401012024")));
        assertTrue(list.stream().anyMatch(t -> t.collegeCode().equals("164") && t.enrollmentNo().equals("01101012024")));
    }

    @Test
    void semesterToppersBySgpa() {
        importFixture();
        List<Toppers.RankTopper> list = toppers.semesterToppers();
        assertEquals(2, list.size()); // one per semester
        assertTrue(list.stream().allMatch(t -> t.enrollmentNo().equals("00401012024")));
    }

    @Test
    void subjectTopperPerPaper() {
        importFixture();
        List<Toppers.SubjectTopper> list = toppers.subjectToppers();
        assertEquals(12, list.size()); // one topper per paper (no ties in the fixture)
        Toppers.SubjectTopper maths1 = list.stream()
                .filter(t -> t.paperId().equals("99101")).findFirst().orElseThrow();
        assertEquals("00101012024", maths1.enrollmentNo()); // Aarav, 90
        assertEquals(90, maths1.totalMarks());
    }

    @Test
    void marksheetForMeera() {
        importFixture();
        Marksheet ms = toppers.marksheet("00401012024");
        assertNotNull(ms);
        assertEquals("MEERA NAIR", ms.name());
        assertFalse(ms.hasBacklog());
        assertEquals(9.86, ms.cgpa(), 0.001);
        assertEquals(2, ms.semesters().size());
        assertEquals(3, ms.semesters().get(0).rows().size());
    }

    @Test
    void backlogStudentHasBacklogFlag() {
        importFixture();
        Marksheet kabir = toppers.marksheet("00301012024");
        assertTrue(kabir.hasBacklog()); // failed Mathematics-I in semester 1
    }
}
