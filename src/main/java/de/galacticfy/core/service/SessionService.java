package de.galacticfy.core.service;

import de.galacticfy.core.database.DatabaseManager;
import org.slf4j.Logger;

import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class SessionService {

    public record SessionInfo(
            UUID uuid,
            String name,
            Instant firstLogin,
            Instant lastLogin,
            Instant lastLogout,
            long totalPlaySeconds,
            String lastServer
    ) {}

    private final DatabaseManager db;
    private final Logger logger;

    public SessionService(DatabaseManager db, Logger logger) {
        this.db = db;
        this.logger = logger;
    }

    public void onLogin(UUID uuid, String name, String serverName) {
        String select = """
                SELECT id, first_login, last_login, last_logout, total_play_seconds, last_server
                FROM gf_sessions
                WHERE uuid = ?
                """;

        String insert = """
                INSERT INTO gf_sessions
                (uuid, name, first_login, last_login, last_server, total_play_seconds)
                VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, 0)
                """;

        String update = """
                UPDATE gf_sessions
                SET name = ?,
                    last_login = CURRENT_TIMESTAMP,
                    last_server = ?
                WHERE uuid = ?
                """;

        try (Connection con = db.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(select)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        // neuer Spieler
                        try (PreparedStatement ins = con.prepareStatement(insert)) {
                            ins.setString(1, uuid.toString());
                            ins.setString(2, name);
                            ins.setString(3, serverName);
                            ins.executeUpdate();
                        }
                    } else {
                        // bestehenden updaten
                        try (PreparedStatement upd = con.prepareStatement(update)) {
                            upd.setString(1, name);
                            upd.setString(2, serverName);
                            upd.setString(3, uuid.toString());
                            upd.executeUpdate();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Fehler beim Session-Login-Update für {}", name, e);
        }
    }

    public void onLogout(UUID uuid) {
        String sql = """
                UPDATE gf_sessions
                SET last_logout = CURRENT_TIMESTAMP,
                    total_play_seconds = total_play_seconds + TIMESTAMPDIFF(SECOND, last_login, CURRENT_TIMESTAMP)
                WHERE uuid = ?
                """;

        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, uuid.toString());
            ps.executeUpdate();

        } catch (SQLException e) {
            logger.error("Fehler beim Session-Logout-Update für {}", uuid, e);
        }
    }

    public SessionInfo getSession(UUID uuid) {
        String sql = """
                SELECT uuid, name, first_login, last_login, last_logout,
                       total_play_seconds, last_server
                FROM gf_sessions
                WHERE uuid = ?
                """;

        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, uuid.toString());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                Timestamp first = rs.getTimestamp("first_login");
                Timestamp last = rs.getTimestamp("last_login");
                Timestamp lastLo = rs.getTimestamp("last_logout");

                return new SessionInfo(
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("name"),
                        first != null ? first.toInstant() : null,
                        last != null ? last.toInstant() : null,
                        lastLo != null ? lastLo.toInstant() : null,
                        rs.getLong("total_play_seconds"),
                        rs.getString("last_server")
                );
            }

        } catch (SQLException e) {
            logger.error("Fehler beim Laden der Session für {}", uuid, e);
            return null;
        }
    }

    public static String formatDuration(long totalSeconds) {
        Duration d = Duration.ofSeconds(totalSeconds);
        long hours = d.toHours();
        long minutes = d.minusHours(hours).toMinutes();
        long seconds = d.minusHours(hours).minusMinutes(minutes).getSeconds();

        if (hours > 0) {
            return String.format("%dh %02dmin %02ds", hours, minutes, seconds);
        }
        if (minutes > 0) {
            return String.format("%dmin %02ds", minutes, seconds);
        }
        return seconds + "s";
    }
}
