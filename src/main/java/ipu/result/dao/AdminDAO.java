package ipu.result.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

/** Admin accounts and login throttling. Manages its own connections via {@link Db}. */
public class AdminDAO {

    private static final int MAX_FAILED = 5;
    private static final long LOCK_MINUTES = 15;

    private final Db db;

    public AdminDAO(Db db) { this.db = db; }

    public record AdminAuth(int id, String username, String passwordHash,
                            int failedLogins, Instant lockedUntil) {
        public boolean isLocked() {
            return lockedUntil != null && lockedUntil.isAfter(Instant.now());
        }
    }

    public boolean exists(String username) {
        try (Connection c = db.open();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM admin WHERE username=?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public void create(String username, String passwordHash) {
        try (Connection c = db.open();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO admin (username, password_hash) VALUES (?,?)")) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public Optional<AdminAuth> find(String username) {
        try (Connection c = db.open();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT admin_id, username, password_hash, failed_logins, locked_until FROM admin WHERE username=?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Timestamp ts = rs.getTimestamp("locked_until");
                return Optional.of(new AdminAuth(rs.getInt(1), rs.getString(2), rs.getString(3),
                        rs.getInt(4), ts == null ? null : ts.toInstant()));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    /** Increments the failed counter; locks the account once the limit is hit. */
    public void recordFailedLogin(String username) {
        try (Connection c = db.open()) {
            int failed;
            try (PreparedStatement ps = c.prepareStatement("SELECT failed_logins FROM admin WHERE username=?")) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return;
                    failed = rs.getInt(1) + 1;
                }
            }
            Timestamp lock = failed >= MAX_FAILED
                    ? Timestamp.from(Instant.now().plusSeconds(LOCK_MINUTES * 60)) : null;
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE admin SET failed_logins=?, locked_until=? WHERE username=?")) {
                ps.setInt(1, failed);
                ps.setTimestamp(2, lock);
                ps.setString(3, username);
                ps.executeUpdate();
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public void resetFailedLogins(String username) {
        try (Connection c = db.open();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE admin SET failed_logins=0, locked_until=NULL WHERE username=?")) {
            ps.setString(1, username);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }
}
