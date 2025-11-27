package de.galacticfy.core.service;

import de.galacticfy.core.database.DatabaseManager;
import org.slf4j.Logger;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class PunishmentService {

    // ============================================================
    // ENUMS & DATENKLASSE
    // ============================================================

    public enum PunishmentType {
        BAN,
        IP_BAN,
        MUTE,
        KICK,
        WARN              // <— neu
    }

    public static class Punishment {
        public final int id;
        public final UUID uuid;
        public final String name;
        public final String ip;
        public final PunishmentType type;
        public final String reason;
        public final String staff;
        public final Instant createdAt;
        public final Instant expiresAt;
        public final boolean active;

        public Punishment(int id,
                          UUID uuid,
                          String name,
                          String ip,
                          PunishmentType type,
                          String reason,
                          String staff,
                          Instant createdAt,
                          Instant expiresAt,
                          boolean active) {
            this.id = id;
            this.uuid = uuid;
            this.name = name;
            this.ip = ip;
            this.type = type;
            this.reason = reason;
            this.staff = staff;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
            this.active = active;
        }
    }

    // ============================================================
    // FELDER
    // ============================================================

    private final DatabaseManager db;
    private final Logger logger;

    public PunishmentService(DatabaseManager db, Logger logger) {
        this.db = db;
        this.logger = logger;
    }

    // ============================================================
    // BAN (Account-Ban über UUID/Name)
    // ============================================================

    public Punishment banPlayer(UUID uuid,
                                String name,
                                String ip,
                                String reason,
                                String staff,
                                Long durationMs) {
        return createPunishment(uuid, name, ip, PunishmentType.BAN, reason, staff, durationMs);
    }

    public boolean unbanPlayer(UUID uuid) {
        if (uuid == null) return false;

        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE gf_punishments SET active = 0 " +
                             "WHERE uuid = ? AND type = 'BAN' AND active = 1"
             )) {
            ps.setString(1, uuid.toString());
            int updated = ps.executeUpdate();
            return updated > 0;
        } catch (SQLException e) {
            logger.error("Fehler beim Unbannen von {}", uuid, e);
            return false;
        }
    }

    public boolean unbanByName(String name) {
        if (name == null || name.isBlank()) return false;
        String key = name.toLowerCase(Locale.ROOT);

        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE gf_punishments SET active = 0 " +
                             "WHERE LOWER(name) = ? AND type = 'BAN' AND active = 1"
             )) {
            ps.setString(1, key);
            int updated = ps.executeUpdate();
            return updated > 0;
        } catch (SQLException e) {
            logger.error("Fehler beim Unbannen von Name {}", name, e);
            return false;
        }
    }

    // ============================================================
    // IP-BAN
    // ============================================================

    public Punishment banIp(String ip,
                            String reason,
                            String staff,
                            Long durationMs) {
        if (ip == null || ip.isBlank()) {
            logger.warn("banIp aufgerufen ohne gültige IP.");
            return null;
        }

        String name = "IP " + ip;
        return createPunishment(null, name, ip, PunishmentType.IP_BAN, reason, staff, durationMs);
    }

    // ============================================================
    // MUTE
    // ============================================================

    public Punishment mutePlayer(UUID uuid,
                                 String name,
                                 String ip,
                                 String reason,
                                 String staff,
                                 Long durationMs) {
        return createPunishment(uuid, name, ip, PunishmentType.MUTE, reason, staff, durationMs);
    }

    public boolean unmutePlayer(UUID uuid) {
        if (uuid == null) return false;

        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE gf_punishments SET active = 0 " +
                             "WHERE uuid = ? AND type = 'MUTE' AND active = 1"
             )) {
            ps.setString(1, uuid.toString());
            int updated = ps.executeUpdate();
            return updated > 0;
        } catch (SQLException e) {
            logger.error("Fehler beim Unmuten von {}", uuid, e);
            return false;
        }
    }

    public boolean unmuteByName(String name) {
        if (name == null || name.isBlank()) return false;
        String key = name.toLowerCase(Locale.ROOT);

        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE gf_punishments SET active = 0 " +
                             "WHERE LOWER(name) = ? AND type = 'MUTE' AND active = 1"
             )) {
            ps.setString(1, key);
            int updated = ps.executeUpdate();
            return updated > 0;
        } catch (SQLException e) {
            logger.error("Fehler beim Unmuten von Name {}", name, e);
            return false;
        }
    }

    // ============================================================
// WARN
// ============================================================

    /**
     * Verwarnung mit optionaler Dauer.
     * durationMs == null oder <= 0  → permanent (keine expires_at)
     */
    public Punishment warnPlayer(UUID uuid,
                                 String name,
                                 String ip,
                                 String reason,
                                 String staff,
                                 Long durationMs) {
        return createPunishment(uuid, name, ip, PunishmentType.WARN, reason, staff, durationMs);
    }


    /** Anzahl aller Warns (egal welcher Grund) für Spieler */
    public int countWarns(UUID uuid, String name) {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM gf_punishments WHERE type = 'WARN' "
        );
        List<Object> params = new ArrayList<>();

        if (uuid != null) {
            sql.append("AND uuid = ? ");
            params.add(uuid.toString());
        } else if (name != null && !name.isBlank()) {
            sql.append("AND LOWER(name) = ? ");
            params.add(name.toLowerCase(Locale.ROOT));
        } else {
            return 0;
        }

        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, (String) params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            logger.error("Fehler beim Zählen der Warns", e);
        }
        return 0;
    }

    /** Liste der letzten Warns für /warnings */
    public List<Punishment> getWarnings(UUID uuid, String name, int limit) {
        List<Punishment> list = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
                "SELECT * FROM gf_punishments WHERE type = 'WARN' "
        );
        List<Object> params = new ArrayList<>();

        if (uuid != null) {
            sql.append("AND uuid = ? ");
            params.add(uuid.toString());
        } else if (name != null && !name.isBlank()) {
            sql.append("AND LOWER(name) = ? ");
            params.add(name.toLowerCase(Locale.ROOT));
        } else {
            return list;
        }

        sql.append("ORDER BY created_at DESC, id DESC LIMIT ?");
        params.add(limit);

        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof String s) {
                    ps.setString(i + 1, s);
                } else if (p instanceof Integer in) {
                    ps.setInt(i + 1, in);
                }
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapPunishment(rs));
                }
            }

        } catch (SQLException e) {
            logger.error("Fehler beim Laden der Warnungs-Liste", e);
        }

        return list;
    }

    // ============================================================
    // KICK (nur History / kein aktiver Punishment)
    // ============================================================

    public Punishment logKick(UUID uuid,
                              String name,
                              String ip,
                              String reason,
                              String staff) {
        return createPunishment(uuid, name, ip, PunishmentType.KICK, reason, staff, null);
    }

    // ============================================================
    // INTERN: Eintrag erstellen
    // ============================================================

    private Punishment createPunishment(UUID uuid,
                                        String name,
                                        String ip,
                                        PunishmentType type,
                                        String reason,
                                        String staff,
                                        Long durationMs) {
        if (name == null || name.isBlank()) {
            if (ip != null && !ip.isBlank()) {
                name = "IP " + ip;
            } else {
                name = "Unknown";
            }
        }

        Timestamp expires = null;
        if (durationMs != null && durationMs > 0) {
            long end = System.currentTimeMillis() + durationMs;
            expires = Timestamp.from(Instant.ofEpochMilli(end));
        }

        Instant now = Instant.now();

        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO gf_punishments " +
                             "(uuid, name, ip, type, reason, staff, created_at, expires_at, active) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1)",
                     Statement.RETURN_GENERATED_KEYS
             )) {

            if (uuid != null) {
                ps.setString(1, uuid.toString());
            } else {
                ps.setNull(1, Types.CHAR);
            }

            ps.setString(2, name);

            if (ip != null && !ip.isBlank()) {
                ps.setString(3, ip);
            } else {
                ps.setNull(3, Types.VARCHAR);
            }

            ps.setString(4, type.name());
            ps.setString(5, reason != null ? reason : "Kein Grund angegeben");
            ps.setString(6, staff != null ? staff : "Konsole");
            ps.setTimestamp(7, Timestamp.from(now));

            if (expires != null) {
                ps.setTimestamp(8, expires);
            } else {
                ps.setNull(8, Types.TIMESTAMP);
            }

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    return new Punishment(
                            id,
                            uuid,
                            name,
                            ip,
                            type,
                            reason,
                            staff,
                            now,
                            expires != null ? expires.toInstant() : null,
                            true
                    );
                }
            }

        } catch (SQLException e) {
            logger.error("Fehler beim Erstellen eines {}-Punishments für {}",
                    type, uuid != null ? uuid : name, e);
        }

        return null;
    }

    // ============================================================
    // ACTIVE BAN/MUTE ABFRAGEN (Login / Chat)
    // ============================================================

    public Punishment getActiveBan(UUID uuid, String ip) {
        // 1) normaler Account-Ban
        Punishment ban = getActivePunishment(PunishmentType.BAN, uuid, null);
        if (ban != null) return ban;

        // 2) IP-Ban
        if (ip != null && !ip.isBlank()) {
            return getActivePunishment(PunishmentType.IP_BAN, null, ip);
        }

        return null;
    }

    public Punishment getActiveMute(UUID uuid) {
        return getActivePunishment(PunishmentType.MUTE, uuid, null);
    }

    private Punishment getActivePunishment(PunishmentType type, UUID uuid, String ip) {
        try (Connection con = db.getConnection()) {

            Punishment p = null;

            // 1) UUID
            if (uuid != null) {
                p = querySingleActive(con, type, "uuid = ?", uuid.toString());
            }

            // 2) IP (optional)
            if (p == null && ip != null && !ip.isBlank()) {
                p = querySingleActive(con, type, "ip = ?", ip);
            }

            if (p == null) return null;

            if (isExpired(p)) {
                deactivateById(p.id);
                return null;
            }

            return p;

        } catch (SQLException e) {
            logger.error("Fehler beim Abfragen von {} für uuid={}, ip={}", type, uuid, ip, e);
        }
        return null;
    }

    private Punishment querySingleActive(Connection con,
                                         PunishmentType type,
                                         String where,
                                         String value) throws SQLException {

        String sql = "SELECT * FROM gf_punishments " +
                "WHERE " + where + " AND type = ? AND active = 1 " +
                "ORDER BY id DESC LIMIT 1";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, value);
            ps.setString(2, type.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapPunishment(rs);
                }
            }
        }
        return null;
    }

    private boolean isExpired(Punishment p) {
        if (p.expiresAt == null) return false;
        return p.expiresAt.toEpochMilli() <= System.currentTimeMillis();
    }

    private void deactivateById(int id) {
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE gf_punishments SET active = 0 WHERE id = ?"
             )) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Fehler beim Deaktivieren von Punishment id={}", id, e);
        }
    }

    private Punishment mapPunishment(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String uuidStr = rs.getString("uuid");
        UUID uuid = null;

        if (uuidStr != null) {
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException ignored) {
            }
        }

        String name = rs.getString("name");
        String ip = rs.getString("ip");
        String typeStr = rs.getString("type");
        PunishmentType type = PunishmentType.valueOf(typeStr.toUpperCase(Locale.ROOT));
        String reason = rs.getString("reason");
        String staff = rs.getString("staff");
        Timestamp createdTs = rs.getTimestamp("created_at");
        Timestamp expiresTs = rs.getTimestamp("expires_at");
        boolean active = rs.getBoolean("active");

        return new Punishment(
                id,
                uuid,
                name,
                ip,
                type,
                reason,
                staff,
                createdTs != null ? createdTs.toInstant() : null,
                expiresTs != null ? expiresTs.toInstant() : null,
                active
        );
    }

    // ============================================================
    // HISTORY
    // ============================================================

    public List<Punishment> getHistory(UUID uuid, String name, int limit) {
        List<Punishment> list = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
                "SELECT * FROM gf_punishments WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        if (uuid != null) {
            sql.append("AND uuid = ? ");
            params.add(uuid.toString());
        } else if (name != null && !name.isBlank()) {
            sql.append("AND LOWER(name) = ? ");
            params.add(name.toLowerCase(Locale.ROOT));
        } else {
            return list;
        }

        sql.append("ORDER BY created_at DESC, id DESC LIMIT ?");
        params.add(limit);

        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof String s) {
                    ps.setString(i + 1, s);
                } else if (p instanceof Integer in) {
                    ps.setInt(i + 1, in);
                }
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapPunishment(rs));
                }
            }

        } catch (SQLException e) {
            logger.error("Fehler beim Laden der Punishment-History", e);
        }

        return list;
    }

    // ============================================================
    // FORMAT-HILFEN
    // ============================================================

    public String formatRemaining(Punishment p) {
        if (p == null || p.expiresAt == null) {
            return "permanent";
        }
        long ms = p.expiresAt.toEpochMilli() - System.currentTimeMillis();
        if (ms <= 0) return "0s";
        return formatDuration(ms);
    }

    public String formatDuration(long millis) {
        long totalSeconds = millis / 1000L;

        long days    = totalSeconds / 86400;
        long hours   = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();

        if (days > 0)    sb.append(days).append("d ");
        if (hours > 0)   sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.isEmpty()) sb.append(seconds).append("s");

        return sb.toString().trim();
    }

    public Long parseDuration(String input) {
        if (input == null || input.isBlank()) return null;

        input = input.toLowerCase(Locale.ROOT).trim();

        long multiplier;
        if (input.endsWith("m"))      { multiplier = 60_000L;        input = input.replace("m", ""); }
        else if (input.endsWith("h")) { multiplier = 3_600_000L;     input = input.replace("h", ""); }
        else if (input.endsWith("d")) { multiplier = 86_400_000L;    input = input.replace("d", ""); }
        else if (input.endsWith("w")) { multiplier = 604_800_000L;   input = input.replace("w", ""); }
        else if (input.endsWith("mo")){ multiplier = 2_592_000_000L; input = input.replace("mo", ""); }
        else if (input.endsWith("y")) { multiplier = 31_536_000_000L;input = input.replace("y", ""); }
        else return null;

        try {
            long num = Long.parseLong(input);
            return num * multiplier;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ============================================================
    // TAB-HILFEN
    // ============================================================

    /** alle bekannten Namen (für /history, /warn, /check, /unban …) */
    public List<String> getAllPunishedNames() {
        List<String> list = new ArrayList<>();
        String sql = "SELECT DISTINCT name FROM gf_punishments ORDER BY name ASC";

        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String name = rs.getString("name");
                if (name != null && !name.isBlank()) {
                    list.add(name);
                }
            }

        } catch (SQLException e) {
            logger.error("Fehler beim Laden aller Punishment-Namen", e);
        }

        return list;
    }

    public List<String> findKnownNames(String prefix, int limit) {
        List<String> list = new ArrayList<>();
        if (limit <= 0) limit = 30;
        if (prefix == null) prefix = "";
        String key = prefix.toLowerCase(Locale.ROOT) + "%";

        String sql = "SELECT DISTINCT name FROM gf_punishments " +
                "WHERE LOWER(name) LIKE ? ORDER BY name ASC LIMIT ?";

        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, key);
            ps.setInt(2, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("name");
                    if (name != null && !name.isBlank()) {
                        list.add(name);
                    }
                }
            }

        } catch (SQLException e) {
            logger.error("Fehler bei findKnownNames(prefix={})", prefix, e);
        }

        return list;
    }

    public List<String> getActiveBannedNames() {
        return getActiveNamesByType(PunishmentType.BAN);
    }

    public List<String> getActiveMutedNames() {
        return getActiveNamesByType(PunishmentType.MUTE);
    }

    private List<String> getActiveNamesByType(PunishmentType type) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT DISTINCT name FROM gf_punishments " +
                "WHERE type = ? AND active = 1 ORDER BY name ASC";

        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, type.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("name");
                    if (name != null && !name.isBlank()) {
                        list.add(name);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Fehler beim Laden aktiver Namen für Typ {}", type, e);
        }

        return list;
    }

    // ============================================================
    // SHUTDOWN
    // ============================================================

    public void shutdown() {
        logger.info("PunishmentService: Shutdown aufgerufen.");
    }
}
