package com.db2h2.connectors;

import com.db2h2.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * H2 database connector implementation
 */
public class H2Connector extends AbstractDatabaseConnector {
    
    private static final Logger logger = LoggerFactory.getLogger(H2Connector.class);
    
    static {
        try {
            // Explicitly load the H2 JDBC driver
            Class.forName("org.h2.Driver");
            logger.debug("H2 JDBC driver loaded successfully");
        } catch (ClassNotFoundException e) {
            logger.error("Failed to load H2 JDBC driver: {}", e.getMessage());
        }
    }
    
    public H2Connector(DatabaseConfig config) {
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