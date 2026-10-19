package ipu.result.dao;

import ipu.result.parser.InternalFixtureGenerator;
import ipu.result.service.ImportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

/** Guards the restart bug: re-running initSchema after data exists must not fail. */
class SchemaInitIdempotentTest {

    @Test
    void initSchemaIsSafeToRepeatWithData(@TempDir Path tmp) throws Exception {
        String url = "jdbc:h2:mem:s" + System.nanoTime()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        Db db = new Db(url, "sa", "");
        db.initSchema();

        File pdf = tmp.resolve("bca.pdf").toFile();
        new InternalFixtureGenerator().generate(pdf, InternalFixtureGenerator.bca());
        new ImportService(db).importPdf(pdf, "bca.pdf", "bca.pdf");

        int before;
        try (Connection c = db.open()) { before = new ResultDAO().count(c, "student"); }
        assertTrue(before > 0);

        // Second start (as happens when the server is restarted) must not throw
        // and must not wipe the grade scale or the imported data.
        assertDoesNotThrow(db::initSchema);

        try (Connection c = db.open()) {
            assertEquals(before, new ResultDAO().count(c, "student"));
            assertEquals(8, new ResultDAO().count(c, "grade_scale"));
        }
    }
}
