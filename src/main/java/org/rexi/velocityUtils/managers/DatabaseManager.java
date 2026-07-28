package org.rexi.velocityUtils.managers;

import org.slf4j.Logger;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {

    private final ConfigManager configManager;
    private final Logger logger;

    public DatabaseManager(ConfigManager configManager, Logger logger) {
        this.configManager = configManager;
        this.logger = logger;
    }

    public void createTables() {
        String dbType = configManager.getString("database.type").toLowerCase();

        if (!dbType.equalsIgnoreCase("mysql")) {
            dbType = "sqlite";
        }

        String staffTimeTable;
        String playerInfoTable;
        String playerBans;

        if (dbType.equals("mysql")) {
            staffTimeTable = """
        CREATE TABLE IF NOT EXISTS staff_time_daily (
            uuid VARCHAR(36) NOT NULL,
            date DATE NOT NULL,
            duration_seconds INT NOT NULL,
            PRIMARY KEY (uuid, date)
        );
        """;

            playerInfoTable = """
        CREATE TABLE IF NOT EXISTS player_info (
            uuid VARCHAR(36) PRIMARY KEY,
            name VARCHAR(16) NOT NULL,
            last_join TIMESTAMP,
            player_ip VARCHAR(45)
        );
        """;

            playerBans = """
        CREATE TABLE IF NOT EXISTS player_bans (
            name VARCHAR(20) PRIMARY KEY,
            ip VARCHAR(45),
            ipban BOOLEAN NOT NULL,
            banned_by VARCHAR(20) NOT NULL,
            banned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            reason VARCHAR(16)
        );
        """;
        } else {
            staffTimeTable = """
        CREATE TABLE IF NOT EXISTS staff_time_daily (
            uuid TEXT NOT NULL,
            date TEXT NOT NULL,
            duration_seconds INTEGER NOT NULL,
            PRIMARY KEY (uuid, date)
        );
        """;

            playerInfoTable = """
        CREATE TABLE IF NOT EXISTS player_info (
            uuid TEXT PRIMARY KEY,
            name TEXT NOT NULL,
            last_join TEXT,
            player_ip TEXT
        );
        """;

            playerBans = """
        CREATE TABLE IF NOT EXISTS player_bans (
            name TEXT PRIMARY KEY,
            ip TEXT,
            ipban BOOLEAN NOT NULL,
            banned_by TEXT NOT NULL,
            banned_at TEXT DEFAULT CURRENT_TIMESTAMP,
            reason TEXT
        );
        """;
        }

        try (var conn = getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute(staffTimeTable);
            stmt.execute(playerInfoTable);
            stmt.execute(playerBans);

            try {
                if (dbType.equals("mysql")) {
                    stmt.execute("ALTER TABLE player_info ADD COLUMN IF NOT EXISTS last_join TIMESTAMP");
                } else {
                    stmt.execute("ALTER TABLE player_info ADD COLUMN last_join TEXT");
                }
            } catch (SQLException ignore) {
                // si ya existe, SQLite lanza error → lo ignoramos
            }
            try {
                if (dbType.equals("mysql")) {
                    stmt.execute("ALTER TABLE player_info ADD COLUMN IF NOT EXISTS player_ip TIMESTAMP");
                } else {
                    stmt.execute("ALTER TABLE player_info ADD COLUMN player_ip TEXT");
                }
            } catch (SQLException ignore) {
                // si ya existe, SQLite lanza error → lo ignoramos
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException {
        String dbType = configManager.getString("database.type").toLowerCase();

        if (!dbType.equalsIgnoreCase("mysql")) {
            dbType = "sqlite";
        }

        if (dbType.equals("mysql")) {
            String host = configManager.getString("database.mysql.host");
            int port = configManager.getInt("database.mysql.port");
            String database = configManager.getString("database.mysql.database");
            String username = configManager.getString("database.mysql.username");
            String password = configManager.getString("database.mysql.password");

            try {
                Class.forName("com.mysql.cj.jdbc.Driver"); // Cargar driver MySQL
            } catch (ClassNotFoundException e) {
                throw new SQLException("MySQL driver not found", e);
            }

            String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true&characterEncoding=utf8";
            return DriverManager.getConnection(url, username, password);
        } else {

            File oldDb = new File("plugins/VelocityUtils/stafftime.db");
            File finalDb = new File("plugins/VelocityUtils/data.db");
            if (oldDb.exists() && !finalDb.exists()) {
                if (oldDb.renameTo(finalDb)) {
                    logger.info("[VelocityUtils] Renaming database to data.db");
                } else {
                    logger.warn("[VelocityUtils] stafftime.db couldnt be renamed");
                    finalDb = oldDb;
                }
            }

            try {
                Class.forName("org.sqlite.JDBC"); // Cargar driver SQLite
            } catch (ClassNotFoundException e) {
                throw new SQLException("SQLite driver not found", e);
            }

            return DriverManager.getConnection("jdbc:sqlite:" + finalDb.getPath());
        }
    }

    public boolean isUsingMySQL() {
        return configManager.getString("database.type")
                .equalsIgnoreCase("mysql");
    }
}
