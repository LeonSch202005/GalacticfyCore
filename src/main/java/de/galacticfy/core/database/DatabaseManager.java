package de.galacticfy.core.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * HikariCP + MariaDB Connection-Pool.
 */
public class DatabaseManager {

    private final Logger logger;
    private HikariDataSource dataSource;

    public DatabaseManager(Logger logger) {
        this.logger = logger;
    }

    public void init() {
        HikariConfig cfg = new HikariConfig();

        // TODO: später aus Config lesen
        cfg.setJdbcUrl("jdbc:mariadb://localhost:3306/galacticfy_core");
        cfg.setUsername("galacticfy");
        cfg.setPassword("bKdHRouvvx0Gds7nEz4XVAh3zp1C2ldH");

        cfg.setDriverClassName("org.mariadb.jdbc.Driver");
        cfg.setMaximumPoolSize(10);
        cfg.setMinimumIdle(2);
        cfg.setPoolName("GalacticfyCorePool");

        cfg.addDataSourceProperty("cachePrepStmts", "true");
        cfg.addDataSourceProperty("prepStmtCacheSize", "250");
        cfg.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        this.dataSource = new HikariDataSource(cfg);
        logger.info("GalacticfyCore: Database-Pool initialisiert.");
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) throw new IllegalStateException("DatabaseManager.init() nicht aufgerufen.");
        return dataSource.getConnection();
    }

    public void shutdown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
