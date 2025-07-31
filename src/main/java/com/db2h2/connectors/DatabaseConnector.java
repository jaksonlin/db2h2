package com.db2h2.connectors;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Interface for database connectors
 */
public interface DatabaseConnector {
    
    /**
     * Connects to the database
     */
    void connect() throws SQLException;
    
    /**
     * Disconnects from the database
     */
    void disconnect();
    
    /**
     * Checks if connected to the database
     */
    boolean isConnected();
    
    /**
     * Gets all table names from the database
     */
    List<String> getTableNames() throws SQLException;
    
    /**
     * Gets metadata for a specific table
     */
    TableMetadata getTableMetadata(String tableName) throws SQLException;
    
    /**
     * Executes a query and returns the result set
     */
    ResultSet executeQuery(String sql) throws SQLException;
    
    /**
     * Executes an update statement
     */
    int executeUpdate(String sql) throws SQLException;
    
    /**
     * Gets the row count for a table
     */
    long getRowCount(String tableName) throws SQLException;
    
    /**
     * Gets data from a table with limit and offset
     */
    ResultSet getTableData(String tableName, int limit, int offset) throws SQLException;
    
    /**
     * Gets the database type
     */
    String getDatabaseType();
    
    /**
     * Gets the database version
     */
    String getDatabaseVersion() throws SQLException;
    
    /**
     * Table metadata class
     */
    class TableMetadata {
        private String tableName;
        private List<ColumnMetadata> columns;
        private List<String> primaryKeys;
        private List<ForeignKeyMetadata> foreignKeys;
        private List<IndexMetadata> indexes;
        
        // Getters and Setters
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        
        public List<ColumnMetadata> getColumns() { return columns; }
        public void setColumns(List<ColumnMetadata> columns) { this.columns = columns; }
        
        public List<String> getPrimaryKeys() { return primaryKeys; }
        public void setPrimaryKeys(List<String> primaryKeys) { this.primaryKeys = primaryKeys; }
        
        public List<ForeignKeyMetadata> getForeignKeys() { return foreignKeys; }
        public void setForeignKeys(List<ForeignKeyMetadata> foreignKeys) { this.foreignKeys = foreignKeys; }
        
        public List<IndexMetadata> getIndexes() { return indexes; }
        public void setIndexes(List<IndexMetadata> indexes) { this.indexes = indexes; }
    }
    
    /**
     * Column metadata class
     */
    class ColumnMetadata {
        private String name;
        private String type;
        private int size;
        private boolean nullable;
        private String defaultValue;
        private boolean autoIncrement;
        
        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
        
        public boolean isNullable() { return nullable; }
        public void setNullable(boolean nullable) { this.nullable = nullable; }
        
        public String getDefaultValue() { return defaultValue; }
        public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
        
        public boolean isAutoIncrement() { return autoIncrement; }
        public void setAutoIncrement(boolean autoIncrement) { this.autoIncrement = autoIncrement; }
    }
    
    /**
     * Foreign key metadata class
     */
    class ForeignKeyMetadata {
        private String name;
        private String columnName;
        private String referencedTable;
        private String referencedColumn;
        
        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getColumnName() { return columnName; }
        public void setColumnName(String columnName) { this.columnName = columnName; }
        
        public String getReferencedTable() { return referencedTable; }
        public void setReferencedTable(String referencedTable) { this.referencedTable = referencedTable; }
        
        public String getReferencedColumn() { return referencedColumn; }
        public void setReferencedColumn(String referencedColumn) { this.referencedColumn = referencedColumn; }
    }
    
    /**
     * Index metadata class
     */
    class IndexMetadata {
        private String name;
        private String columnName;
        private boolean unique;
        
        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getColumnName() { return columnName; }
        public void setColumnName(String columnName) { this.columnName = columnName; }
        
        public boolean isUnique() { return unique; }
        public void setUnique(boolean unique) { this.unique = unique; }
    }
} 