package de.galacticfy.core.database;

import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Legt Tabellen für Rollen, User-Rollen, Permissions, Inheritance
 * und Maintenance-Konfiguration an.
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

            logger.info("GalacticfyCore: DB-Migrationen erfolgreich.");
        } catch (SQLException e) {
            logger.error("Fehler bei DB-Migrationen", e);
        }
    }
}
