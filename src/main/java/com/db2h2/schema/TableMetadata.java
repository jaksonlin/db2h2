package com.db2h2.schema;

import java.util.List;

/**
 * Table metadata class
 */
public class TableMetadata {
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