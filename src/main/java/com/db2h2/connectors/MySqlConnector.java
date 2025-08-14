package com.db2h2.connectors;

import com.db2h2.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MySQL database connector implementation
 */
public class MySqlConnector extends AbstractDatabaseConnector {
    
    private static final Logger logger = LoggerFactory.getLogger(MySqlConnector.class);
    
    static {
        try {
            // Explicitly load the MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            logger.debug("MySQL JDBC driver loaded successfully");
        } catch (ClassNotFoundException e) {
            logger.error("Failed to load MySQL JDBC driver: {}", e.getMessage());
        }
    }
    
    public MySqlConnector(DatabaseConfig config) {
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