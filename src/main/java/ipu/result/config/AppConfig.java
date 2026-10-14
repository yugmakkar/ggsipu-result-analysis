package ipu.result.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Loads configuration from config.properties (in the working directory) when
 * present, otherwise falls back to an embedded H2 database so the app and the
 * tests run with no external setup. Values can also be overridden by env vars
 * (DB_URL, DB_USER, DB_PASSWORD, WEB_PORT, UPLOAD_DIR).
 */
public class AppConfig {

    private final Properties props = new Properties();

    public AppConfig() {
        Path file = Path.of("config.properties");
        if (Files.isReadable(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                props.load(in);
            } catch (IOException e) {
                throw new RuntimeException("failed to read config.properties", e);
            }
        }
    }

    public String dbUrl() {
        // Default: embedded H2 in MySQL-compatibility mode, stored under ./data.
        return pick("DB_URL", "db.url",
                "jdbc:h2:file:./data/ipu;MODE=MySQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE");
    }

    public String dbUser()     { return pick("DB_USER", "db.user", "sa"); }
    public String dbPassword() { return pick("DB_PASSWORD", "db.password", ""); }

    public int webPort() {
        return Integer.parseInt(pick("WEB_PORT", "web.port", "8080"));
    }

    public String uploadDir() {
        return pick("UPLOAD_DIR", "upload.dir", "uploads");
    }

    public long uploadMaxBytes() {
        return Long.parseLong(pick("UPLOAD_MAX_BYTES", "upload.maxBytes", "20971520"));
    }

    private String pick(String env, String key, String dflt) {
        String e = System.getenv(env);
        if (e != null && !e.isBlank()) return e;
        return props.getProperty(key, dflt);
    }
}
