package com.db2h2.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for loading JDBC drivers
 */
public class DriverLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(DriverLoader.class);
    
    /**
     * Loads the appropriate JDBC driver based on database type
     * @param databaseType the type of database (postgresql, mysql, sqlserver, oracle, h2)
     */
    public static void loadDriver(String databaseType) {
        try {
            switch (databaseType.toLowerCase()) {
                case "postgresql":
                    Class.forName("org.postgresql.Driver");
                    logger.debug("PostgreSQL JDBC driver loaded successfully");
                    break;
                case "mysql":
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    logger.debug("MySQL JDBC driver loaded successfully");
                    break;
                case "sqlserver":
                case "mssql":
                    Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
                    logger.debug("SQL Server JDBC driver loaded successfully");
                    break;
                case "oracle":
                    Class.forName("oracle.jdbc.driver.OracleDriver");
                    logger.debug("Oracle JDBC driver loaded successfully");
                    break;
                case "h2":
                    Class.forName("org.h2.Driver");
                    logger.debug("H2 JDBC driver loaded successfully");
                    break;
                default:
                    logger.warn("Unknown database type: {}. Driver loading skipped.", databaseType);
            }
        } catch (ClassNotFoundException e) {
            logger.error("Failed to load JDBC driver for {}: {}", databaseType, e.getMessage());
        }
    }
    
    /**
     * Loads all available JDBC drivers
     */
    public static void loadAllDrivers() {
        loadDriver("postgresql");
        loadDriver("mysql");
        loadDriver("sqlserver");
        loadDriver("oracle");
        loadDriver("h2");
    }
}
