package com.db2h2.core;

import com.db2h2.config.MigrationConfig;
import com.db2h2.connectors.DatabaseConnector;
import com.db2h2.schema.TableMetadata;
import com.db2h2.schema.ColumnMetadata;
import com.db2h2.schema.ForeignKeyMetadata;
import com.db2h2.schema.IndexMetadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles schema migration from source database to H2
 */
public class SchemaMigrator {
    
    private static final Logger logger = LoggerFactory.getLogger(SchemaMigrator.class);
    
    private final DatabaseConnector sourceConnector;
    private final DatabaseConnector targetConnector;
    private final MigrationConfig config;
    
    // Function mappings from source database to H2
    private final Map<String, String> functionMappings;
    
    public SchemaMigrator(DatabaseConnector sourceConnector, DatabaseConnector targetConnector, 
                         MigrationConfig config) {
        this.sourceConnector = sourceConnector;
        this.targetConnector = targetConnector;
        this.config = config;
        this.functionMappings = initializeFunctionMappings();
    }
    
    /**
     * Initializes function mappings from source database to H2
     */
    private Map<String, String> initializeFunctionMappings() {
        Map<String, String> mappings = new HashMap<>();
        
        // PostgreSQL to H2 function mappings
        if ("postgresql".equalsIgnoreCase(sourceConnector.getDatabaseType())) {
            mappings.put("gen_random_uuid()", "RANDOM_UUID()");
            mappings.put("CURRENT_TIMESTAMP", "CURRENT_TIMESTAMP");
            mappings.put("now()", "CURRENT_TIMESTAMP");
            mappings.put("true", "true");
            mappings.put("false", "false");
        }
        
        // MySQL to H2 function mappings
        if ("mysql".equalsIgnoreCase(sourceConnector.getDatabaseType())) {
            mappings.put("CURRENT_TIMESTAMP", "CURRENT_TIMESTAMP");
            mappings.put("NOW()", "CURRENT_TIMESTAMP");
            mappings.put("UUID()", "RANDOM_UUID()");
        }
        
        // Oracle to H2 function mappings
        if ("oracle".equalsIgnoreCase(sourceConnector.getDatabaseType())) {
            mappings.put("SYSDATE", "CURRENT_TIMESTAMP");
            mappings.put("SYSTIMESTAMP", "CURRENT_TIMESTAMP");
            mappings.put("SYS_GUID()", "RANDOM_UUID()");
        }
        
        // SQL Server to H2 function mappings
        if ("sqlserver".equalsIgnoreCase(sourceConnector.getDatabaseType()) || 
            "microsoft".equalsIgnoreCase(sourceConnector.getDatabaseType())) {
            mappings.put("GETDATE()", "CURRENT_TIMESTAMP");
            mappings.put("GETUTCDATE()", "CURRENT_TIMESTAMP");
            mappings.put("NEWID()", "RANDOM_UUID()");
        }
        
        // Add custom function mappings from configuration if they exist
        if (config.getMigration().getFunctionMappings() != null) {
            mappings.putAll(config.getMigration().getFunctionMappings());
            logger.debug("Added {} custom function mappings from configuration", 
                        config.getMigration().getFunctionMappings().size());
        }
        
        logger.debug("Initialized {} function mappings for database type: {}", 
                    mappings.size(), sourceConnector.getDatabaseType());
        return mappings;
    }
    
    /**
     * Migrates the schema for the specified tables
     */
    public void migrateSchema(List<String> tableNames) throws SQLException {
        logger.info("Starting schema migration for {} tables", tableNames.size());
        logger.info("Using {} function mappings for database type: {}", 
                   functionMappings.size(), sourceConnector.getDatabaseType());
        
        // Log function mappings for debugging
        if (logger.isDebugEnabled()) {
            for (Map.Entry<String, String> mapping : functionMappings.entrySet()) {
                logger.debug("Function mapping: {} -> {}", mapping.getKey(), mapping.getValue());
            }
        }
        
        // Create tables in dependency order
        List<String> orderedTables = orderTablesByDependencies(tableNames);
        
        for (String tableName : orderedTables) {
            try {
                migrateTable(tableName);
                logger.info("Successfully migrated schema for table: {}", tableName);
            } catch (Exception e) {
                logger.error("Failed to migrate schema for table {}: {}", tableName, e.getMessage());
                if (config.getOutput().getExitOnError()) {
                    throw e;
                }
            }
        }
        
        // Add foreign key constraints after all tables are created
        if (config.getMigration().getConstraints().getPreserveForeignKeys()) {
            addForeignKeyConstraints(tableNames);
        }
        
        logger.info("Schema migration completed");
    }
    
    /**
     * Validates and logs column metadata for debugging
     */
    private void logColumnMetadata(TableMetadata metadata) {
        logger.info("Processing table: {} with {} columns", metadata.getTableName(), metadata.getColumns().size());
        
        for (ColumnMetadata column : metadata.getColumns()) {
            String mappedType = mapDataType(column.getType(), column.getSize());
            logger.info("Column: {} - Source Type: {} - Size: {} - Mapped to: {} - Nullable: {}", 
                       column.getName(), column.getType(), column.getSize(), mappedType, column.isNullable());
            
            // Warn about potentially problematic columns
            if (column.getSize() > 1000000) {
                logger.warn("Large column detected: {} with size {} - will be converted to CLOB", 
                           column.getName(), column.getSize());
            }
        }
    }
    
    /**
     * Validates and sanitizes CREATE TABLE SQL for H2 compatibility
     */
    private String sanitizeCreateTableSql(String sql) {
        // Check for any remaining problematic patterns
        if (sql.contains("VARCHAR(2147483647)")) {
            logger.error("Found problematic VARCHAR(2147483647) in SQL - this should not happen!");
            throw new IllegalStateException("VARCHAR precision overflow detected in generated SQL");
        }
        
        // Additional H2-specific validations can be added here
        logger.debug("SQL validation passed");
        return sql;
    }
    
    /**
     * Migrates a single table
     */
    private void migrateTable(String tableName) throws SQLException {
        logger.debug("Migrating table: {}", tableName);
        
        // Check if table already exists
        if (tableExists(tableName)) {
            logger.info("Table {} already exists, dropping and recreating", tableName);
            dropTable(tableName);
        }
        
        // Get table metadata from source
        TableMetadata metadata = sourceConnector.getTableMetadata(tableName);
        
        // Log column metadata for debugging
        logColumnMetadata(metadata);
        
        // Generate CREATE TABLE statement
        String createTableSql = generateCreateTableSql(metadata);
        
        // Validate and sanitize the SQL
        createTableSql = sanitizeCreateTableSql(createTableSql);
        
        // Execute CREATE TABLE
        targetConnector.executeUpdate(createTableSql);
        
        // Create indexes
        if (config.getMigration().getConstraints().getPreserveIndexes()) {
            createIndexes(metadata);
        }
    }
    
    /**
     * Checks if a table already exists
     */
	private boolean tableExists(String tableName) {
		try {
			// Use JDBC metadata for portability across databases
			java.sql.DatabaseMetaData md = targetConnector.getConnection().getMetaData();
			try (java.sql.ResultSet rs = md.getTables(null, null, tableName.toUpperCase(), new String[] { "TABLE" })) {
				return rs.next();
			}
		} catch (Exception e) {
			logger.debug("Could not check if table exists: {}", e.getMessage());
		}
		return false;
	}
    
    /**
     * Drops a table if it exists
     */
    private void dropTable(String tableName) throws SQLException {
        try {
            String sql = "DROP TABLE IF EXISTS " + tableName;
            targetConnector.executeUpdate(sql);
            logger.debug("Dropped existing table: {}", tableName);
        } catch (Exception e) {
            logger.warn("Failed to drop table {}: {}", tableName, e.getMessage());
        }
    }
    
    /**
     * Translates default values from source database to H2-compatible ones
     */
    public String translateDefaultValue(String defaultValue) {
        if (defaultValue == null) {
            return null;
        }
        
        String translated = defaultValue;
        
        // Apply function mappings first
        for (Map.Entry<String, String> mapping : functionMappings.entrySet()) {
            if (translated.contains(mapping.getKey())) {
                translated = translated.replace(mapping.getKey(), mapping.getValue());
            }
        }
        
        // Handle PostgreSQL specific patterns first (before general type casting removal)
        if ("postgresql".equalsIgnoreCase(sourceConnector.getDatabaseType())) {
            // Convert '{}'::jsonb to '{}' for H2
            if (translated.contains("'{}'::jsonb")) {
                translated = translated.replace("'{}'::jsonb", "'{}'");
            }
            
            // Convert 'member'::character varying to 'member' for H2
            if (translated.contains("::character varying")) {
                translated = translated.replace("::character varying", "");
            }
            
            // Handle numeric defaults with type casting
            if (translated.contains("::numeric")) {
                translated = translated.replace("::numeric", "");
            }
            
            if (translated.contains("::integer")) {
                translated = translated.replace("::integer", "");
            }
            
            if (translated.contains("::text")) {
                translated = translated.replace("::text", "");
            }
            
            if (translated.contains("::boolean")) {
                translated = translated.replace("::boolean", "");
            }
        }
        
        // Handle PostgreSQL array syntax (e.g., '{}'::text[] -> '{}')
        if (translated.contains("[]")) {
            translated = translated.replaceAll("\\[\\]", "");
        }
        
        // Clean up any remaining type casting artifacts with a more precise regex
        // This should only catch remaining ::type patterns that weren't handled above
        translated = translated.replaceAll("::[a-zA-Z_][a-zA-Z0-9_]*", "");
        
        logger.debug("Translated default value: {} -> {}", defaultValue, translated);
        return translated;
    }

    /**
     * Generates CREATE TABLE SQL for H2
     */
    private String generateCreateTableSql(TableMetadata metadata) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ").append(metadata.getTableName()).append(" (");
        
        // Add columns
        for (int i = 0; i < metadata.getColumns().size(); i++) {
            ColumnMetadata column = metadata.getColumns().get(i);
            
            if (i > 0) {
                sql.append(", ");
            }
            
            String mappedType = mapDataType(column.getType(), column.getSize());
            logger.debug("Mapping column {}: {} (size: {}) -> {}", 
                       column.getName(), column.getType(), column.getSize(), mappedType);
            
            sql.append(column.getName()).append(" ");
            sql.append(mappedType);
            
            if (!column.isNullable()) {
                sql.append(" NOT NULL");
            }
            
            if (column.getDefaultValue() != null) {
                String translatedDefault = translateDefaultValue(column.getDefaultValue());
                if (translatedDefault != null) {
                    sql.append(" DEFAULT ").append(translatedDefault);
                }
            }
            
            if (column.isAutoIncrement()) {
                sql.append(" AUTO_INCREMENT");
            }
        }
        
        // Add primary key
        if (!metadata.getPrimaryKeys().isEmpty()) {
            sql.append(", PRIMARY KEY (");
            for (int i = 0; i < metadata.getPrimaryKeys().size(); i++) {
                if (i > 0) {
                    sql.append(", ");
                }
                sql.append(metadata.getPrimaryKeys().get(i));
            }
            sql.append(")");
        }
        
        sql.append(")");
        
        logger.debug("Generated CREATE TABLE SQL: {}", sql.toString());
        return sql.toString();
    }
    
    /**
     * Determines if a column size represents an unlimited or very large field
     */
    public boolean isUnlimitedOrVeryLargeSize(int size) {
        String dbType = sourceConnector.getDatabaseType();
        logger.debug("Checking size {} for database type: {}", size, dbType);
        
        // Common values used by different databases to represent unlimited text
        boolean isUnlimited = size <= 0 || 
               size == 2147483647 ||  // PostgreSQL unlimited VARCHAR/TEXT, MySQL LONGTEXT
               size == 65535 ||       // MySQL TEXT
               size == 16777215 ||    // MySQL MEDIUMTEXT
               size > config.getMigration().getMaxVarcharSizeThreshold(); // Configurable threshold
        
        if (isUnlimited) {
            logger.debug("Column size {} identified as unlimited/very large for database type: {}", size, dbType);
        }
        
        return isUnlimited;
    }
    
    /**
     * Maps data types from source to H2
     */
    public String mapDataType(String sourceType, int size) {
        String type = sourceType.toUpperCase();
        
        // Check for custom mappings
        if (config.getMigration().getDataTypeMappings() != null) {
            String customMapping = config.getMigration().getDataTypeMappings().get(type);
            if (customMapping != null) {
                return customMapping;
            }
        }
        
        // Default mappings
        switch (type) {
            case "VARCHAR":
            case "CHAR":
            case "TEXT":
            case "STRING":
                // Handle unlimited or very large text fields
                if (isUnlimitedOrVeryLargeSize(size)) {
                    // For unlimited text or very large sizes, use CLOB in H2
                    // This handles various database unlimited size representations
                    logger.debug("Converting large/unlimited text field (size: {}) to CLOB for type: {}", size, type);
                    return "CLOB";
                }
                return "VARCHAR(" + size + ")";
                
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
    
    /**
     * Creates indexes for a table
     */
    private void createIndexes(TableMetadata metadata) throws SQLException {
        logger.info("Creating indexes for table: {}", metadata.getTableName());
        
        for (IndexMetadata index : metadata.getIndexes()) {
            try {
                // Check if index already exists
                if (indexExists(metadata.getTableName().toUpperCase(), index.getName().toUpperCase())) {
                    logger.debug("Index {} already exists on table {}, skipping", index.getName(), metadata.getTableName());
                    continue;
                }
                
                // Check if the column type supports indexing in H2
                if (!isIndexableColumnType(metadata.getTableName(), index.getColumnName())) {
                    logger.warn("Skipping index {} on column {} - column type not supported for indexing in H2", 
                               index.getName(), index.getColumnName());
                    continue;
                }
                
                String indexSql = generateCreateIndexSql(metadata.getTableName(), index);
                targetConnector.executeUpdate(indexSql);
                logger.debug("Created index: {} on table: {}", index.getName(), metadata.getTableName());
            } catch (Exception e) {
                logger.warn("Failed to create index {} on table {}: {}", 
                           index.getName(), metadata.getTableName(), e.getMessage());
            }
        }
    }
    
    /**
     * Checks if a column type supports indexing in H2
     */
    private boolean isIndexableColumnType(String tableName, String columnName) {
        try {
            // Get column metadata to check the type
            TableMetadata tableMetadata = sourceConnector.getTableMetadata(tableName);
            for (ColumnMetadata column : tableMetadata.getColumns()) {
                if (column.getName().equalsIgnoreCase(columnName)) {
                    String columnType = column.getType().toUpperCase();
                    // H2 doesn't support indexes on CLOB, BLOB, and some other types
                    return !columnType.contains("CLOB") && 
                           !columnType.contains("BLOB") && 
                           !columnType.contains("LONG") &&
                           !columnType.contains("TEXT");
                }
            }
        } catch (Exception e) {
            logger.debug("Could not determine column type for {} in table {}: {}", columnName, tableName, e.getMessage());
        }
        // Default to true if we can't determine the type
        return true;
    }
    
    /**
     * Checks if an index already exists on a table
     */
    private boolean indexExists(String tableName, String indexName) {
        try {
            String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.INDEXES WHERE TABLE_NAME = ? AND INDEX_NAME = ?";
            try (java.sql.PreparedStatement pstmt = targetConnector.getConnection().prepareStatement(sql)) {
                pstmt.setString(1, tableName.toUpperCase());
                pstmt.setString(2, indexName.toUpperCase());
                try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1) > 0;
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Could not check if index exists: {}", e.getMessage());
        }
        return false;
    }
    
    /**
     * Generates CREATE INDEX SQL
     */
    private String generateCreateIndexSql(String tableName, IndexMetadata index) {
        StringBuilder sql = new StringBuilder();
        
        if (index.isUnique()) {
            sql.append("CREATE UNIQUE INDEX ");
        } else {
            sql.append("CREATE INDEX ");
        }
        
        sql.append(index.getName()).append(" ON ");
        sql.append(tableName).append(" (");
        sql.append(index.getColumnName()).append(")");
        
        return sql.toString();
    }
    
    /**
     * Adds foreign key constraints
     */
    private void addForeignKeyConstraints(List<String> tableNames) throws SQLException {
        logger.info("Adding foreign key constraints...");
        
        for (String tableName : tableNames) {
            TableMetadata metadata = sourceConnector.getTableMetadata(tableName);
            
            for (ForeignKeyMetadata fk : metadata.getForeignKeys()) {
                try {
                    // Check if foreign key constraint already exists
                    if (foreignKeyExists(tableName, fk.getName())) {
                        logger.debug("Foreign key {} already exists on table {}, skipping", fk.getName(), tableName);
                        continue;
                    }
                    
                    String fkSql = generateForeignKeySql(tableName, fk);
                    targetConnector.executeUpdate(fkSql);
                    logger.debug("Added foreign key: {} on table: {}", fk.getName(), tableName);
                } catch (Exception e) {
                    logger.warn("Failed to add foreign key {} on table {}: {}", 
                               fk.getName(), tableName, e.getMessage());
                }
            }
        }
    }
    
    /**
     * Checks if a foreign key constraint already exists
     */
    private boolean foreignKeyExists(String tableName, String constraintName) {
        try {
            // For H2, use the CONSTRAINTS table which contains table information
            String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS   WHERE TABLE_NAME = ? AND CONSTRAINT_NAME = ? AND CONSTRAINT_TYPE = 'FOREIGN KEY'";
            try (java.sql.PreparedStatement pstmt = targetConnector.getConnection().prepareStatement(sql)) {
                pstmt.setString(1, tableName.toUpperCase());
                pstmt.setString(2, constraintName.toUpperCase());
                try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1) > 0;
                    }
                }
            }
            
        } catch (Exception e) {
            logger.debug("Could not check if foreign key exists: {}", e.getMessage());
        }
        return false;
    }
    
    /**
     * Generates foreign key constraint SQL
     */
    private String generateForeignKeySql(String tableName, ForeignKeyMetadata fk) {
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE ").append(tableName);
        sql.append(" ADD CONSTRAINT ").append(fk.getName());
        sql.append(" FOREIGN KEY (").append(fk.getColumnName()).append(")");
        sql.append(" REFERENCES ").append(fk.getReferencedTable());
        sql.append(" (").append(fk.getReferencedColumn()).append(")");
        
        return sql.toString();
    }
    
    /**
     * Orders tables by dependencies (tables with foreign keys come after referenced tables)
     */
    private List<String> orderTablesByDependencies(List<String> tableNames) throws SQLException {
        // Simple implementation - in a real scenario, you might want to use a topological sort
        // For now, we'll just return the tables in the order they were provided
        // and handle foreign key constraints separately
        return tableNames;
    }
    
    /**
     * Updates sequences (for auto-increment columns)
     */
    public void updateSequences() throws SQLException {
        logger.info("Updating sequences...");
        
        // H2 handles auto-increment automatically, so this is mainly for logging
        logger.debug("Sequences will be handled automatically by H2");
    }
} 