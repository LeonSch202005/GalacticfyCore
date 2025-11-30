package de.galacticfy.core.database;

import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Legt Tabellen für Rollen, User-Rollen, Permissions, Inheritance,
 * Maintenance-Konfiguration, Punishments, Reports, Economy, Sessions, Daily-Rewards und NPCs an.
 */
public class DatabaseMigrationService {

    private final DatabaseManager db;
    private final Logger logger;

    public DatabaseMigrationService(DatabaseManager db, Logger logger) {
        this.db = db;
        this.logger = logger;
    }

    public void runMigrations() {
        try (Connection con = db.getConnection(); Statement st = con.createStatement()) {

            // ===========================
            // ROLLEN
            // ===========================
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS gf_roles (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(64) NOT NULL UNIQUE,
                        display_name VARCHAR(64) NOT NULL,
                        color_hex VARCHAR(16) NULL,
                        prefix VARCHAR(128) NULL,
                        suffix VARCHAR(128) NULL,
                        is_staff TINYINT(1) NOT NULL DEFAULT 0,
                        maintenance_bypass TINYINT(1) NOT NULL DEFAULT 0,
                        join_priority INT NOT NULL DEFAULT 0
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                    """);

            // ===========================
            // USER → ROLLEN (+ Expire)
            // ===========================
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS gf_user_roles (
                        uuid CHAR(36) NOT NULL PRIMARY KEY,
                        name VARCHAR(16) NOT NULL,
                        role_id INT NOT NULL,
                        expires_at TIMESTAMP NULL DEFAULT NULL,
                        FOREIGN KEY (role_id) REFERENCES gf_roles(id) ON DELETE CASCADE
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                    """);

            // ===========================
            // ROLLEN-PERMISSIONS
            // ===========================
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS gf_role_permissions (
                        role_id INT NOT NULL,
                        permission VARCHAR(128) NOT NULL,
                        PRIMARY KEY (role_id, permission),
                        FOREIGN KEY (role_id) REFERENCES gf_roles(id) ON DELETE CASCADE
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                    """);

            // ===========================
            // ROLLEN-VERERBUNG (INHERIT)
            // ===========================
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS gf_role_inherits (
                        role_id INT NOT NULL,
                        parent_role_id INT NOT NULL,
                        PRIMARY KEY (role_id, parent_role_id),
                        FOREIGN KEY (role_id) REFERENCES gf_roles(id) ON DELETE CASCADE,
                        FOREIGN KEY (parent_role_id) REFERENCES gf_roles(id) ON DELETE CASCADE
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                    """);

            // ===========================
            // SESSIONS (/seen, /check)
            // ===========================
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS gf_sessions (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        uuid CHAR(36) NOT NULL,
                        name VARCHAR(16) NOT NULL,
                        first_login TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        last_login  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        last_logout TIMESTAMP NULL DEFAULT NULL,
                        total_play_seconds BIGINT NOT NULL DEFAULT 0,
                        last_server VARCHAR(64) NULL,
                        INDEX idx_sessions_uuid (uuid),
                        INDEX idx_sessions_name (name)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                    """);

            // ===========================
            // GLOBAL ECONOMY (Galas + Stardust)
            // ===========================
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS gf_economy (
                        uuid CHAR(36) NOT NULL PRIMARY KEY,
                        name VARCHAR(16) NOT NULL,
                        balance BIGINT NOT NULL DEFAULT 0,      -- Galas
                        stardust BIGINT NOT NULL DEFAULT 0,     -- Premium: Stardust ✧
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                            ON UPDATE CURRENT_TIMESTAMP
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                    """);

            // Falls gf_economy schon existiert aber noch kein 'stardust' hat → nachziehen
            try {
                st.executeUpdate("""
                        ALTER TABLE gf_economy
                            ADD COLUMN IF NOT EXISTS stardust BIGINT NOT NULL DEFAULT 0
                        """);
            } catch (SQLException e) {
                logger.debug("gf_economy: Spalte 'stardust' existiert evtl. bereits.", e);
            }

            // ===========================
            // DAILY REWARDS
            // ===========================
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS gf_daily_rewards (
                        uuid CHAR(36) NOT NULL PRIMARY KEY,
                        name VARCHAR(16) NOT NULL,
                        last_claim_date DATE NOT NULL,
                        streak INT NOT NULL DEFAULT 1,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                            ON UPDATE CURRENT_TIMESTAMP
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                    """);

            // ===========================
            // MAINTENANCE CONFIG
            // ===========================
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS gf_maintenance_config (
                        id INT PRIMARY KEY,
                        enabled TINYINT(1) NOT NULL DEFAULT 0,
                        end_at TIMESTAMP NULL DEFAULT NULL
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                    """);

            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS gf_maintenance_whitelist_players (
                        name VARCHAR(64) NOT NULL PRIMARY KEY
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                    """);

            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS gf_maintenance_whitelist_groups (
                        group_name VARCHAR(64) NOT NULL PRIMARY KEY
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                    """);

            // ===========================
// QUESTS (Definitionen)
// ===========================
            st.executeUpdate("""
        CREATE TABLE IF NOT EXISTS gf_quests (
            quest_key      VARCHAR(64) NOT NULL PRIMARY KEY,
            title          VARCHAR(128) NOT NULL,
            description    TEXT NOT NULL,
            type           VARCHAR(32) NOT NULL, -- z.B. PLAYTIME_MINUTES, DAILY_LOGIN, EARN_GALAS
            goal           BIGINT NOT NULL,
            reward_galas   BIGINT NOT NULL DEFAULT 0,
            reward_stardust BIGINT NOT NULL DEFAULT 0,
            active         TINYINT(1) NOT NULL DEFAULT 1,
            created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """);

// ===========================
// QUEST PROGRESS (pro Spieler)
// ===========================
            st.executeUpdate("""
        CREATE TABLE IF NOT EXISTS gf_quest_progress (
            uuid         CHAR(36) NOT NULL,
            quest_key    VARCHAR(64) NOT NULL,
            progress     BIGINT NOT NULL DEFAULT 0,
            completed    TINYINT(1) NOT NULL DEFAULT 0,
            completed_at TIMESTAMP NULL DEFAULT NULL,
            last_update  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            PRIMARY KEY (uuid, quest_key),
            FOREIGN KEY (quest_key) REFERENCES gf_quests(quest_key) ON DELETE CASCADE
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """);

            // ===========================
            // PUNISHMENTS (Bans + Mutes + Kicks + Warns + IP-Bans)
            // ===========================
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS gf_punishments (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        uuid CHAR(36) NULL,
                        name VARCHAR(16) NOT NULL,
                        ip VARCHAR(45) NULL,
                        type VARCHAR(16) NOT NULL, -- BAN, IP_BAN, MUTE, KICK, WARN
                        reason VARCHAR(255) NOT NULL,
                        staff VARCHAR(32) NOT NULL,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        expires_at TIMESTAMP NULL DEFAULT NULL,
                        active TINYINT(1) NOT NULL DEFAULT 1,
                        INDEX idx_punish_uuid_type_active (uuid, type, active),
                        INDEX idx_punish_ip_type_active (ip, type, active),
                        INDEX idx_punish_name (name)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                    """);

            // ===========================
            // REPORTS (/report)
            // ===========================
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS gf_reports (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        reporter_name VARCHAR(16) NOT NULL,
                        target_name  VARCHAR(16) NOT NULL,
                        server_name  VARCHAR(64),
                        reason       TEXT NOT NULL,
                        preset_key   VARCHAR(64),
                        handled      TINYINT(1) NOT NULL DEFAULT 0,
                        handled_by   VARCHAR(32) NULL,
                        handled_at   TIMESTAMP NULL DEFAULT NULL,
                        INDEX idx_reports_target (target_name),
                        INDEX idx_reports_created_at (created_at),
                        INDEX idx_reports_handled (handled)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                    """);

            // für bereits existierende gf_reports Tabellen: Spalten nachziehen
            try {
                st.executeUpdate("""
                        ALTER TABLE gf_reports
                            ADD COLUMN IF NOT EXISTS handled TINYINT(1) NOT NULL DEFAULT 0
                        """);
            } catch (SQLException e) {
                logger.debug("gf_reports: Spalte 'handled' existiert evtl. bereits.", e);
            }

            try {
                st.executeUpdate("""
                        ALTER TABLE gf_reports
                            ADD COLUMN IF NOT EXISTS handled_by VARCHAR(32) NULL
                        """);
            } catch (SQLException e) {
                logger.debug("gf_reports: Spalte 'handled_by' existiert evtl. bereits.", e);
            }

            try {
                st.executeUpdate("""
                        ALTER TABLE gf_reports
                            ADD COLUMN IF NOT EXISTS handled_at TIMESTAMP NULL
                        """);
            } catch (SQLException e) {
                logger.debug("gf_reports: Spalte 'handled_at' existiert evtl. bereits.", e);
            }

            // ===========================
            // NPCS (für Lobby-/Spigot-Plugin)
            // Gemeinsames Schema für GalacticfyCore + GalacticfyChat
            // ===========================
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS gf_npcs (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        server_name  VARCHAR(64) NOT NULL,   -- z.B. "Lobby-1"
                        name         VARCHAR(64) NOT NULL,   -- Anzeigename über dem Kopf
                        world        VARCHAR(64) NOT NULL,
                        x            DOUBLE NOT NULL,
                        y            DOUBLE NOT NULL,
                        z            DOUBLE NOT NULL,
                        yaw          FLOAT NOT NULL,
                        pitch        FLOAT NOT NULL,
                        type         VARCHAR(32) NOT NULL,   -- z.B. SERVER_SELECTOR, INFO, ...
                        target_server VARCHAR(64) NULL,      -- z.B. "Citybuild-1"
                        skin_uuid    CHAR(36) NULL,
                        created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                    """);

            logger.info("GalacticfyCore: DB-Migrationen erfolgreich.");
        } catch (SQLException e) {
            logger.error("Fehler bei DB-Migrationen", e);
        }
    }
}
