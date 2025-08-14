package com.db2h2.connectors;

import com.db2h2.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Oracle database connector implementation
 */
public class OracleConnector extends AbstractDatabaseConnector {
    
    private static final Logger logger = LoggerFactory.getLogger(OracleConnector.class);
    
    static {
        try {
            // Explicitly load the Oracle JDBC driver
            Class.forName("oracle.jdbc.driver.OracleDriver");
            logger.debug("Oracle JDBC driver loaded successfully");
        } catch (ClassNotFoundException e) {
            logger.error("Failed to load Oracle JDBC driver: {}", e.getMessage());
        }
    }
    
    public OracleConnector(DatabaseConfig config) {
        super(config);
    }
    
    @Override
    protected String buildSelectQuery(String tableName, int limit, int offset) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM (");
        sql.append("SELECT a.*, ROWNUM rnum FROM (");
        sql.append("SELECT * FROM ").append(tableName);
        sql.append(") a WHERE ROWNUM <= ").append(offset + limit);
        sql.append(") WHERE rnum > ").append(offset);
        
        return sql.toString();
    }
} 