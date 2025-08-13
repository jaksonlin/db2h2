package com.db2h2.connectors;

import com.db2h2.config.DatabaseConfig;
import com.db2h2.schema.TableMetadata;
import com.db2h2.schema.ColumnMetadata;
import com.db2h2.schema.ForeignKeyMetadata;
import com.db2h2.schema.IndexMetadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Abstract base class for database connectors
 */
public abstract class AbstractDatabaseConnector implements DatabaseConnector {
    
    protected static final Logger logger = LoggerFactory.getLogger(AbstractDatabaseConnector.class);
    
    protected DatabaseConfig config;
    protected Connection connection;
    protected DatabaseMetaData metaData;

    public Connection getConnection() {
        return connection;
    }
    
    public AbstractDatabaseConnector(DatabaseConfig config) {
        this.config = config;
    }
    
    @Override
    public void connect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            logger.debug("Connection already established");
            return;
        }
        
        String jdbcUrl = config.buildJdbcUrl();
        logger.info("Connecting to database: {}", jdbcUrl);
        
        try {
            connection = DriverManager.getConnection(jdbcUrl, config.getUsername(), config.getPassword());
            metaData = connection.getMetaData();
            logger.info("Successfully connected to database: {}", config.getDatabase());
        } catch (SQLException e) {
            logger.error("Failed to connect to database: {}", e.getMessage());
            throw e;
        }
    }
    
    @Override
    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
                logger.info("Disconnected from database");
            } catch (SQLException e) {
                logger.warn("Error closing connection: {}", e.getMessage());
            } finally {
                connection = null;
                metaData = null;
            }
        }
    }
    
    @Override
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
    
    @Override
    public List<String> getTableNames() throws SQLException {
        if (!isConnected()) {
            connect();
        }
        
        List<String> tableNames = new ArrayList<>();
        try (ResultSet rs = metaData.getTables(config.getDatabase(), null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                tableNames.add(tableName);
            }
        }
        
        logger.info("Found {} tables in database", tableNames.size());
        return tableNames;
    }
    
    @Override
    public TableMetadata getTableMetadata(String tableName) throws SQLException {
        if (!isConnected()) {
            connect();
        }
        
        TableMetadata metadata = new TableMetadata();
        metadata.setTableName(tableName);
        
        // Get column information
        List<ColumnMetadata> columns = new ArrayList<>();
        try (ResultSet rs = metaData.getColumns(config.getDatabase(), null, tableName, "%")) {
            while (rs.next()) {
                ColumnMetadata column = new ColumnMetadata();
                column.setName(rs.getString("COLUMN_NAME"));
                column.setType(rs.getString("TYPE_NAME"));
                column.setSize(rs.getInt("COLUMN_SIZE"));
                column.setNullable(rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                column.setDefaultValue(rs.getString("COLUMN_DEF"));
                column.setAutoIncrement("YES".equals(rs.getString("IS_AUTOINCREMENT")));
                columns.add(column);
            }
        }
        metadata.setColumns(columns);
        
        // Get primary key information
        List<String> primaryKeys = new ArrayList<>();
        try (ResultSet rs = metaData.getPrimaryKeys(config.getDatabase(), null, tableName)) {
            while (rs.next()) {
                primaryKeys.add(rs.getString("COLUMN_NAME"));
            }
        }
        metadata.setPrimaryKeys(primaryKeys);
        
        // Get foreign key information
        List<ForeignKeyMetadata> foreignKeys = new ArrayList<>();
        try (ResultSet rs = metaData.getImportedKeys(config.getDatabase(), null, tableName)) {
            while (rs.next()) {
                ForeignKeyMetadata fk = new ForeignKeyMetadata();
                fk.setName(rs.getString("FK_NAME"));
                fk.setColumnName(rs.getString("FKCOLUMN_NAME"));
                fk.setReferencedTable(rs.getString("PKTABLE_NAME"));
                fk.setReferencedColumn(rs.getString("PKCOLUMN_NAME"));
                foreignKeys.add(fk);
            }
        }
        metadata.setForeignKeys(foreignKeys);
        
        // Get index information
        List<IndexMetadata> indexes = new ArrayList<>();
        try (ResultSet rs = metaData.getIndexInfo(config.getDatabase(), null, tableName, false, false)) {
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                if (indexName != null && !indexName.equalsIgnoreCase("PRIMARY")) {
                    IndexMetadata index = new IndexMetadata();
                    index.setName(indexName);
                    index.setColumnName(rs.getString("COLUMN_NAME"));
                    index.setUnique(!rs.getBoolean("NON_UNIQUE"));
                    indexes.add(index);
                }
            }
        }
        metadata.setIndexes(indexes);
        
        logger.debug("Retrieved metadata for table: {}", tableName);
        return metadata;
    }
    
    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        if (!isConnected()) {
            connect();
        }
        
        logger.debug("Executing query: {}", sql);
        Statement stmt = connection.createStatement();
        return stmt.executeQuery(sql);
    }
    
    @Override
    public int executeUpdate(String sql) throws SQLException {
        if (!isConnected()) {
            connect();
        }
        
        logger.debug("Executing update: {}", sql);
        try (Statement stmt = connection.createStatement()) {
            return stmt.executeUpdate(sql);
        }
    }
    
    @Override
    public long getRowCount(String tableName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + tableName;
        try (ResultSet rs = executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        return 0;
    }
    
    @Override
    public ResultSet getTableData(String tableName, int limit, int offset) throws SQLException {
        String sql = buildSelectQuery(tableName, limit, offset);
        return executeQuery(sql);
    }
    
    @Override
    public String getDatabaseType() {
        return config.getType();
    }
    
    @Override
    public String getDatabaseVersion() throws SQLException {
        if (!isConnected()) {
            connect();
        }
        return metaData.getDatabaseProductVersion();
    }
    
    /**
     * Builds a SELECT query with limit and offset
     */
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
    
    /**
     * Maps database-specific data types to H2 equivalents
     */
    protected String mapDataType(String sourceType, int size) {
        String type = sourceType.toUpperCase();
        
        switch (type) {
            case "VARCHAR":
            case "CHAR":
            case "TEXT":
            case "STRING":
                return size > 0 ? "VARCHAR(" + size + ")" : "VARCHAR";
                
            case "INT":
            case "INTEGER":
            case "SMALLINT":
            case "TINYINT":
                return "INT";
                
            case "BIGINT":
            case "LONG":
                return "BIGINT";
                
            case "DECIMAL":
            case "NUMERIC":
                return "DECIMAL";
                
            case "FLOAT":
            case "REAL":
                return "FLOAT";
                
            case "DOUBLE":
                return "DOUBLE";
                
            case "BOOLEAN":
            case "BOOL":
                return "BOOLEAN";
                
            case "DATE":
                return "DATE";
                
            case "TIME":
                return "TIME";
                
            case "TIMESTAMP":
            case "DATETIME":
                return "TIMESTAMP";
                
            case "BLOB":
            case "BINARY":
                return "BLOB";
                
            case "CLOB":
                return "CLOB";
                
            case "UUID":
                return "UUID";
                
            default:
                logger.warn("Unknown data type: {}, using VARCHAR", type);
                return "VARCHAR";
        }
    }
    
   
} 