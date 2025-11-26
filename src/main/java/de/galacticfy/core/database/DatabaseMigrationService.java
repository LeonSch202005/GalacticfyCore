package de.galacticfy.core.database;

import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Legt Tabellen für Rollen, User-Rollen und Rollen-Permissions an.
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

            // Rollen
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS gf_roles (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(64) NOT NULL UNIQUE,
                        display_name VARCHAR(64) NOT NULL,
                        color_hex VARCHAR(16) NULL,
                        prefix VARCHAR(128) NULL,
                        is_staff TINYINT(1) NOT NULL DEFAULT 0,
                        maintenance_bypass TINYINT(1) NOT NULL DEFAULT 0,
                        join_priority INT NOT NULL DEFAULT 0
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                    """);

            // User-Rollen
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS gf_user_roles (
                        uuid CHAR(36) NOT NULL PRIMARY KEY,
                        name VARCHAR(16) NOT NULL,
                        role_id INT NOT NULL,
                        FOREIGN KEY (role_id) REFERENCES gf_roles(id) ON DELETE CASCADE
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                    """);

            // Rollen-Permissions
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS gf_role_permissions (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        role_id INT NOT NULL,
                        permission VARCHAR(128) NOT NULL,
                        CONSTRAINT uq_role_perm UNIQUE (role_id, permission),
                        FOREIGN KEY (role_id) REFERENCES gf_roles(id) ON DELETE CASCADE
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                    """);

            logger.info("GalacticfyCore: DB-Migrationen erfolgreich.");
        } catch (SQLException e) {
            logger.error("Fehler bei DB-Migrationen", e);
        }
    }
}
