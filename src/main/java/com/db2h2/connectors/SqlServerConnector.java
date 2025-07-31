package com.db2h2.connectors;

import com.db2h2.config.DatabaseConfig;

/**
 * SQL Server database connector implementation
 */
public class SqlServerConnector extends AbstractDatabaseConnector {
    
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