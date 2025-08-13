package com.db2h2.connectors;

import com.db2h2.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SQL Server database connector implementation
 */
public class SqlServerConnector extends AbstractDatabaseConnector {
    
    private static final Logger logger = LoggerFactory.getLogger(SqlServerConnector.class);
    
    static {
        try {
            // Explicitly load the SQL Server JDBC driver
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            logger.debug("SQL Server JDBC driver loaded successfully");
        } catch (ClassNotFoundException e) {
            logger.error("Failed to load SQL Server JDBC driver: {}", e.getMessage());
        }
    }
    
    public SqlServerConnector(DatabaseConfig config) {
        super(config);
    }
    
    @Override
    protected String buildSelectQuery(String tableName, int limit, int offset) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(tableName);
        
        if (limit > 0) {
            sql.append(" OFFSET ").append(offset).append(" ROWS");
            sql.append(" FETCH NEXT ").append(limit).append(" ROWS ONLY");
        }
        
        return sql.toString();
    }
} 