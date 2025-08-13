package com.db2h2.schema;


/**
 * Foreign key metadata class
 */
public class ForeignKeyMetadata {
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
