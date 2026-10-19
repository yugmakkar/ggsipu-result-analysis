package ipu.result.dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/**
 * Owns the JDBC connection details and creates the schema. One instance per
 * application; hand its {@link #open()} connections to the DAOs.
 *
 * The same schema.sql/seed.sql run on both MySQL and H2 (MySQL mode), so this
 * class is dialect-neutral.
 */
public class Db {

    private final String url;
    private final String user;
    private final String password;

    public Db(String url, String user, String password) {
        this.url = Objects.requireNonNull(url);
        this.user = user;
        this.password = password;
    }

    public Connection open() throws SQLException {
        Connection c = DriverManager.getConnection(url, user, password);
        c.setAutoCommit(true);
        return c;
    }

    /**
     * Creates the tables/view (idempotent) and seeds the grade scale once.
     *
     * The seed is applied only when {@code grade_scale} is empty. Re-seeding on
     * every start used to {@code DELETE FROM grade_scale}, which fails once any
     * mark references a grade (the server then could not restart after an
     * import). Running it only when empty keeps init safe to repeat.
     */
    public void initSchema() {
        try (Connection c = open()) {
            runScript(c, "db/schema.sql");
            if (isEmpty(c, "grade_scale")) {
                runScript(c, "db/seed.sql");
            }
        } catch (SQLException | IOException e) {
            throw new RuntimeException("schema init failed", e);
        }
    }

    private boolean isEmpty(Connection c, String table) throws SQLException {
        try (Statement st = c.createStatement();
             var rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return rs.next() && rs.getInt(1) == 0;
        }
    }

    private void runScript(Connection c, String resource) throws SQLException, IOException {
        String sql = readResource(resource);
        // Strip full-line comments first, then split into statements on ';'.
        StringBuilder clean = new StringBuilder();
        for (String line : sql.split("\\r?\\n")) {
            String t = line.trim();
            if (t.isEmpty() || t.startsWith("--")) continue;
            clean.append(line).append('\n');
        }
        try (Statement st = c.createStatement()) {
            for (String stmt : clean.toString().split(";")) {
                String s = stmt.trim();
                if (!s.isEmpty()) st.execute(s);
            }
        }
    }

    private String readResource(String name) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(name)) {
            if (in == null) throw new IOException("resource not found on classpath: " + name);
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}
