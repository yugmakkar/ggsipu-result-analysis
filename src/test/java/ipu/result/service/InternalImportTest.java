package ipu.result.service;

import ipu.result.dao.Db;
import ipu.result.dao.ResultDAO;
import ipu.result.dto.Marksheet;
import ipu.result.parser.InternalFixtureGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

class InternalImportTest {

    private Db db;
    private ImportService importer;
    @TempDir Path tmp;

    @BeforeEach
    void setUp() {
        String url = "jdbc:h2:mem:i" + System.nanoTime()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        db = new Db(url, "sa", "");
        db.initSchema();
        importer = new ImportService(db);
    }

    private File pdf(String name, InternalFixtureGenerator.CourseSpec spec) throws Exception {
        File f = tmp.resolve(name).toFile();
        new InternalFixtureGenerator().generate(f, spec);
        return f;
    }

    private int count(String table) throws Exception {
        try (Connection c = db.open()) { return new ResultDAO().count(c, table); }
    }

    @Test
    void importsInternalMarksForOneCourse() throws Exception {
        var s = importer.importPdf(pdf("bca.pdf", InternalFixtureGenerator.bca()), "bca.pdf", "bca.pdf");
        assertEquals(4, s.students());
        assertEquals(8, s.marks());
        assertEquals(0, s.semesterResults());        // internal import computes no SGPA
        assertEquals(4, count("student"));
        assertEquals(2, count("subject"));
        assertEquals(8, count("marks"));
    }

    @Test
    void marksheetShowsInternalOnly() throws Exception {
        importer.importPdf(pdf("bca.pdf", InternalFixtureGenerator.bca()), "bca.pdf", "bca.pdf");
        try (Connection c = db.open()) {
            Marksheet ms = new ResultDAO().marksheet(c, "00399902023"); // ROHAN KUMAR VERMA
            assertNotNull(ms);
            assertEquals("ROHAN KUMAR VERMA", ms.name());
            assertTrue(ms.internalOnly());
            assertEquals(1, ms.semesters().size());
            assertEquals(2, ms.semesters().get(0).rows().size());
            var lab = ms.semesters().get(0).rows().stream()
                    .filter(r -> r.paperCode().equals("BCAP312")).findFirst().orElseThrow();
            assertEquals(40, lab.internalMax());
            assertNotNull(lab.internal());
        }
    }

    @Test
    void reuploadingSameSheetDoesNotDuplicate() throws Exception {
        importer.importPdf(pdf("bca.pdf", InternalFixtureGenerator.bca()), "bca.pdf", "bca.pdf");
        importer.importPdf(pdf("bca2.pdf", InternalFixtureGenerator.bca()), "bca.pdf", "bca2.pdf");
        assertEquals(4, count("student"));
        assertEquals(8, count("marks"));
    }

    @Test
    void secondCourseLeavesFirstUntouched() throws Exception {
        importer.importPdf(pdf("bca.pdf", InternalFixtureGenerator.bca()), "bca.pdf", "bca.pdf");
        importer.importPdf(pdf("mca.pdf", InternalFixtureGenerator.mca()), "mca.pdf", "mca.pdf");
        assertEquals(7, count("student"));   // 4 BCA + 3 MCA
        assertEquals(4, count("subject"));
        assertEquals(14, count("marks"));    // 8 + 6
        try (Connection c = db.open()) {
            assertNotNull(new ResultDAO().marksheet(c, "00399902023")); // BCA student still present
            assertEquals(2, new ResultDAO().listProgrammes(c).size());  // BCA + MCA
        }
    }

    @Test
    void emptyInternalImportIsRejected() {
        var parsed = new ipu.result.parser.InternalAssessmentExtractor()
                .extract("Internal Assessment for Examination JUNE, 2026\nno rows here\n");
        assertThrows(IllegalArgumentException.class,
                () -> importer.persistInternal(parsed, "x.pdf", "x.pdf"));
    }
}
