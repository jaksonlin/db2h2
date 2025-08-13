package com.db2h2.schema;

/**
 * Column metadata class
 */
public class ColumnMetadata {
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
