package de.galacticfy.core.service;

import de.galacticfy.core.database.DatabaseManager;
import org.slf4j.Logger;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReportService {

    public record ReportEntry(
            long id,
            String targetName,
            String reporterName,
            String reason,
            String serverName,
            String presetKey,
            LocalDateTime createdAt
    ) {}

    private final DatabaseManager db;
    private final Logger logger;

    public ReportService(DatabaseManager db, Logger logger) {
        this.db = db;
        this.logger = logger;
    }

    // ============================================================
    // NEUEN REPORT SPEICHERN
    // ============================================================

    public void addReport(String targetName,
                          String reporterName,
                          String reason,
                          String serverName,
                          String presetKey) {
        if (targetName == null || targetName.isBlank()) return;
        if (reporterName == null || reporterName.isBlank()) reporterName = "Unbekannt";
        if (reason == null || reason.isBlank()) reason = "Kein Grund angegeben";

        String sql = """
                INSERT INTO gf_reports
                (created_at, reporter_name, target_name, server_name, reason, preset_key)
                VALUES (CURRENT_TIMESTAMP, ?, ?, ?, ?, ?)
                """;

        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, reporterName);
            ps.setString(2, targetName);
            ps.setString(3, serverName);
            ps.setString(4, reason);
            if (presetKey != null && !presetKey.isBlank()) {
                ps.setString(5, presetKey.toLowerCase(Locale.ROOT));
            } else {
                ps.setNull(5, Types.VARCHAR);
            }

            ps.executeUpdate();

        } catch (SQLException e) {
            logger.error("Fehler beim Speichern eines Reports (target={}, reporter={})",
                    targetName, reporterName, e);
        }
    }

    // ============================================================
    // REPORTS LADEN
    // ============================================================

    public List<ReportEntry> getReportsFor(String targetName) {
        List<ReportEntry> out = new ArrayList<>();
        if (targetName == null || targetName.isBlank()) return out;

        String sql = """
                SELECT id, created_at, reporter_name, target_name, server_name, reason, preset_key
                FROM gf_reports
                WHERE LOWER(target_name) = ?
                ORDER BY created_at DESC, id DESC
                """;

        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, targetName.toLowerCase(Locale.ROOT));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(mapEntry(rs));
                }
            }

        } catch (SQLException e) {
            logger.error("Fehler beim Laden der Reports für {}", targetName, e);
        }

        return out;
    }

    public List<ReportEntry> getAllReports() {
        List<ReportEntry> out = new ArrayList<>();

        String sql = """
                SELECT id, created_at, reporter_name, target_name, server_name, reason, preset_key
                FROM gf_reports
                ORDER BY created_at DESC, id DESC
                """;

        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                out.add(mapEntry(rs));
            }

        } catch (SQLException e) {
            logger.error("Fehler beim Laden aller Reports", e);
        }

        return out;
    }

    // ============================================================
    // COUNT + CLEAR
    // ============================================================

    public int countAllReports() {
        String sql = "SELECT COUNT(*) FROM gf_reports";

        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            logger.error("Fehler bei countAllReports()", e);
        }
        return 0;
    }

    public boolean clearReportsFor(String targetName) {
        if (targetName == null || targetName.isBlank()) return false;

        String sql = "DELETE FROM gf_reports WHERE LOWER(target_name) = ?";

        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, targetName.toLowerCase(Locale.ROOT));
            int updated = ps.executeUpdate();
            return updated > 0;

        } catch (SQLException e) {
            logger.error("Fehler beim Löschen der Reports für {}", targetName, e);
            return false;
        }
    }

    public int clearAll() {
        String sql = "DELETE FROM gf_reports";

        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            return ps.executeUpdate();

        } catch (SQLException e) {
            logger.error("Fehler beim Löschen aller Reports", e);
            return 0;
        }
    }

    // ============================================================
    // HILFE FÜR TAB-COMPLETE (reported Spieler)
    // ============================================================

    public List<String> getReportedTargetNames() {
        List<String> out = new ArrayList<>();

        String sql = """
                SELECT DISTINCT target_name
                FROM gf_reports
                ORDER BY target_name ASC
                """;

        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String name = rs.getString("target_name");
                if (name != null && !name.isBlank()) {
                    out.add(name);
                }
            }

        } catch (SQLException e) {
            logger.error("Fehler beim Laden der Report-Zielnamen", e);
        }

        return out;
    }

    // ============================================================
    // INTERN
    // ============================================================

    private ReportEntry mapEntry(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        Timestamp ts = rs.getTimestamp("created_at");
        LocalDateTime created = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();

        String reporter = rs.getString("reporter_name");
        String target = rs.getString("target_name");
        String server = rs.getString("server_name");
        String reason = rs.getString("reason");
        String presetKey = rs.getString("preset_key");

        return new ReportEntry(
                id,
                target,
                reporter,
                reason,
                server,
                presetKey,
                created
        );
    }
}
