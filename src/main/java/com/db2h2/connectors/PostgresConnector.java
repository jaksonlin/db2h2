package com.db2h2.connectors;

import com.db2h2.config.DatabaseConfig;

/**
 * PostgreSQL database connector implementation
 */
public class PostgresConnector extends AbstractDatabaseConnector {
    
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