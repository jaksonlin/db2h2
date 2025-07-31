package com.db2h2.core;

import com.db2h2.config.MigrationConfig;
import com.db2h2.connectors.DatabaseConnector;
import com.db2h2.connectors.DatabaseConnector.TableMetadata;
import com.db2h2.connectors.DatabaseConnector.ColumnMetadata;
import com.db2h2.connectors.DatabaseConnector.ForeignKeyMetadata;
import com.db2h2.connectors.DatabaseConnector.IndexMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

/**
 * Handles schema migration from source database to H2
 */
public class SchemaMigrator {
    
    private static final Logger logger = LoggerFactory.getLogger(SchemaMigrator.class);
    
    private final DatabaseConnector sourceConnector;
    private final DatabaseConnector targetConnector;
    private final MigrationConfig config;
    
    public SchemaMigrator(DatabaseConnector sourceConnector, DatabaseConnector targetConnector, 
                         MigrationConfig config) {
        this.sourceConnector = sourceConnector;
        this.targetConnector = targetConnector;
        this.config = config;
    }
    
    /**
     * Migrates the schema for the specified tables
     */
    public void migrateSchema(List<String> tableNames) throws SQLException {
        logger.info("Starting schema migration for {} tables", tableNames.size());
        
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
     * Migrates a single table
     */
    private void migrateTable(String tableName) throws SQLException {
        logger.debug("Migrating table: {}", tableName);
        
        // Get table metadata from source
        TableMetadata metadata = sourceConnector.getTableMetadata(tableName);
        
        // Generate CREATE TABLE statement
        String createTableSql = generateCreateTableSql(metadata);
        
        // Execute CREATE TABLE
        targetConnector.executeUpdate(createTableSql);
        
        // Create indexes
        if (config.getMigration().getConstraints().getPreserveIndexes()) {
            createIndexes(metadata);
        }
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
            
            sql.append(column.getName()).append(" ");
            sql.append(mapDataType(column.getType(), column.getSize()));
            
            if (!column.isNullable()) {
                sql.append(" NOT NULL");
            }
            
            if (column.getDefaultValue() != null) {
                sql.append(" DEFAULT ").append(column.getDefaultValue());
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
     * Maps data types from source to H2
     */
    private String mapDataType(String sourceType, int size) {
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
    
    /**
     * Creates indexes for a table
     */
    private void createIndexes(TableMetadata metadata) throws SQLException {
        for (IndexMetadata index : metadata.getIndexes()) {
            try {
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