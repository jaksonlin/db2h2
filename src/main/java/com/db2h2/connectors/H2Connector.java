package com.db2h2.connectors;

import com.db2h2.config.DatabaseConfig;

/**
 * H2 database connector implementation
 */
public class H2Connector extends AbstractDatabaseConnector {
    
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