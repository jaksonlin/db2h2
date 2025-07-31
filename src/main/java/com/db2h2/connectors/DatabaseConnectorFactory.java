package com.db2h2.connectors;

import com.db2h2.config.DatabaseConfig;

/**
 * Factory class for creating database connectors
 */
public class DatabaseConnectorFactory {
    
    /**
     * Creates a database connector based on the configuration
     */
    public static DatabaseConnector createConnector(DatabaseConfig config) {
        String type = config.getType().toLowerCase();
        
        switch (type) {
            case "postgresql":
            case "postgres":
                return new PostgresConnector(config);
                
            case "mysql":
                return new MySqlConnector(config);
                
            case "oracle":
                return new OracleConnector(config);
                
            case "sqlserver":
            case "mssql":
                return new SqlServerConnector(config);
                
            case "h2":
                return new H2Connector(config);
                
            default:
                throw new IllegalArgumentException("Unsupported database type: " + type);
        }
    }
} 