package ipu.result;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The SQL run at runtime (src/main/resources/db) must stay identical to the
 * human-facing copies in sql/. This guards against the two drifting apart.
 */
class SchemaParityTest {

    @Test
    void schemaAndSeedMatch() throws Exception {
        assertSame("schema.sql");
        assertSame("seed.sql");
    }

    private void assertSame(String name) throws Exception {
        Path a = Path.of("sql", name);
        Path b = Path.of("src", "main", "resources", "db", name);
        assertEquals(norm(Files.readString(a)), norm(Files.readString(b)),
                name + " differs between sql/ and src/main/resources/db/");
    }

    private String norm(String s) {
        return s.replace("\r\n", "\n").strip();
    }
}
