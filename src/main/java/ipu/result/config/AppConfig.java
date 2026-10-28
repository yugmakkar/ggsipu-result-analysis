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

    /**
     * A documented demo admin, seeded on {@code web} startup so anyone can clone
     * the project and log in to try the upload flow without first running
     * {@code create-admin}. These credentials are public on purpose; turn the
     * seeding off on any real deployment with {@code DEMO_ADMIN=false} (see
     * {@link #demoAdmin()}) and create a private admin instead.
     */
    public static final String DEMO_ADMIN_USER = "admin";
    public static final String DEMO_ADMIN_PASSWORD = "admin12345";

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

    /**
     * Topper analytics (overall / subject-wise / semester-wise) are under
     * development and switched off by default. The engine and queries already
     * exist; set features.toppers=true next semester to turn the pages on.
     */
    public boolean toppersEnabled() {
        return Boolean.parseBoolean(pick("FEATURES_TOPPERS", "features.toppers", "false"));
    }

    /**
     * Whether to seed the public {@link #DEMO_ADMIN_USER} account on web startup.
     * On by default for easy evaluation; set {@code DEMO_ADMIN=false} (or
     * {@code demo.admin=false}) on a real deployment.
     */
    public boolean demoAdmin() {
        return Boolean.parseBoolean(pick("DEMO_ADMIN", "demo.admin", "true"));
    }

    private String pick(String env, String key, String dflt) {
        String e = System.getenv(env);
        if (e != null && !e.isBlank()) return e;
        return props.getProperty(key, dflt);
    }
}
