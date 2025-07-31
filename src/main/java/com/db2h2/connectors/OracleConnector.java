package com.db2h2.connectors;

import com.db2h2.config.DatabaseConfig;

/**
 * Oracle database connector implementation
 */
public class OracleConnector extends AbstractDatabaseConnector {
    
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