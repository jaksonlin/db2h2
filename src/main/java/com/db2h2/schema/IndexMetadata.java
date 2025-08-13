package com.db2h2.schema;


/**
 * Index metadata class
 */
public class IndexMetadata {
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
