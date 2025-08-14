package com.db2h2.connectors;

import com.db2h2.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PostgreSQL database connector implementation
 */
public class PostgresConnector extends AbstractDatabaseConnector {
    
    private static final Logger logger = LoggerFactory.getLogger(PostgresConnector.class);
    
    static {
        try {
            // Explicitly load the PostgreSQL JDBC driver
            Class.forName("org.postgresql.Driver");
            logger.debug("PostgreSQL JDBC driver loaded successfully");
        } catch (ClassNotFoundException e) {
            logger.error("Failed to load PostgreSQL JDBC driver: {}", e.getMessage());
        }
    }
    
    public PostgresConnector(DatabaseConfig config) {
        super(config);
    }
    
    @Override
    protected String buildSelectQuery(String tableName, int limit, int offset) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(tableName);
        
        if (limit > 0) {
            sql.append(" LIMIT ").append(limit);
        }
        
        if (offset > 0) {
            sql.append(" OFFSET ").append(offset);
        }
        
        return sql.toString();
    }
} 