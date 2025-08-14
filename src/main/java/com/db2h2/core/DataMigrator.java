package com.db2h2.core;

import com.db2h2.config.MigrationConfig;
import com.db2h2.connectors.DatabaseConnector;
import com.db2h2.schema.TableMetadata;
import com.db2h2.schema.ColumnMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.List;

/**
 * Handles data migration from source database to H2
 */
public class DataMigrator {
    
    private static final Logger logger = LoggerFactory.getLogger(DataMigrator.class);
    
    private final DatabaseConnector sourceConnector;
    private final DatabaseConnector targetConnector;
    private final MigrationConfig config;
    
    public DataMigrator(DatabaseConnector sourceConnector, DatabaseConnector targetConnector, 
                       MigrationConfig config) {
        this.sourceConnector = sourceConnector;
        this.targetConnector = targetConnector;
        this.config = config;
    }
    
    /**
     * Migrates data for the specified tables
     */
    public void migrateData(List<String> tableNames) throws SQLException {
        logger.info("Starting data migration for {} tables", tableNames.size());
        
        for (String tableName : tableNames) {
            try {
                migrateTableData(tableName);
                logger.info("Successfully migrated data for table: {}", tableName);
            } catch (Exception e) {
                logger.error("Failed to migrate data for table {}: {}", tableName, e.getMessage());
                if (config.getOutput().getExitOnError()) {
                    throw e;
                }
            }
        }
        
        logger.info("Data migration completed");
    }
    
    /**
     * Migrates data for a single table
     */
    private void migrateTableData(String tableName) throws SQLException {
        logger.debug("Migrating data for table: {}", tableName);
        
        // Get table metadata
        TableMetadata metadata = sourceConnector.getTableMetadata(tableName);
        
        // Get row count
        long totalRows = sourceConnector.getRowCount(tableName);
        logger.info("Table {} has {} rows", tableName, totalRows);
        
        // Check if we should limit the number of rows
        Integer maxRows = config.getMigration().getData().getMaxRows();
        if (maxRows != null && maxRows > 0 && totalRows > maxRows) {
            logger.info("Limiting migration to {} rows for table {}", maxRows, tableName);
            totalRows = maxRows;
        }
        
        // Check if we should sample data
        if (config.getMigration().getData().getSampleData()) {
            int samplePercentage = config.getMigration().getData().getSamplePercentage();
            long sampleRows = (totalRows * samplePercentage) / 100;
            logger.info("Sampling {}% of data ({} rows) for table {}", samplePercentage, sampleRows, tableName);
            totalRows = sampleRows;
        }
        
        if (totalRows == 0) {
            logger.info("No data to migrate for table: {}", tableName);
            return;
        }
        
        // Migrate data in batches
        int batchSize = config.getMigration().getBatchSize();
        int offset = 0;
        int migratedRows = 0;
        
        while (migratedRows < totalRows) {
            int currentBatchSize = Math.min(batchSize, (int) (totalRows - migratedRows));
            
            try (ResultSet rs = sourceConnector.getTableData(tableName, currentBatchSize, offset)) {
                int batchRows = insertBatchData(tableName, metadata, rs);
                migratedRows += batchRows;
                offset += batchRows;
                
                logger.debug("Migrated batch: {}/{} rows for table {}", migratedRows, totalRows, tableName);
                
                if (batchRows == 0) {
                    break; // No more data
                }
            }
        }
        
        logger.info("Completed data migration for table {}: {} rows", tableName, migratedRows);
    }
    
    /**
     * Inserts a batch of data into the target table
     */
    private int insertBatchData(String tableName, TableMetadata metadata, ResultSet rs) throws SQLException {
        if (!rs.next()) {
            return 0;
        }
        
        // Generate INSERT statement
        String insertSql = generateInsertSql(tableName, metadata);
        
        // Prepare statement with transaction management
        Connection targetConnection = getTargetConnection();
        boolean originalAutoCommit = targetConnection.getAutoCommit();
        
        try {
            targetConnection.setAutoCommit(false);
            
            try (PreparedStatement pstmt = targetConnection.prepareStatement(insertSql)) {
                int rowCount = 0;
                do {
                    // Set parameter values
                    setParameters(pstmt, metadata, rs);
                    
                    // Add to batch
                    pstmt.addBatch();
                    rowCount++;
                    
                } while (rs.next());
                
                // Execute batch
                int[] results = pstmt.executeBatch();
                targetConnection.commit();
                
                // Count successful inserts
                int successfulInserts = 0;
                for (int result : results) {
                    if (result >= 0) {
                        successfulInserts++;
                    }
                }
                
                logger.debug("Inserted {} rows into table {}", successfulInserts, tableName);
                return successfulInserts;
            }
        } catch (SQLException e) {
            logger.error("Error inserting batch data for table {}: {}", tableName, e.getMessage());
            try {
                targetConnection.rollback();
            } catch (SQLException rollbackEx) {
                logger.error("Error rolling back transaction: {}", rollbackEx.getMessage());
            }
            throw e;
        } finally {
            try {
                targetConnection.setAutoCommit(originalAutoCommit);
            } catch (SQLException e) {
                logger.warn("Error restoring auto-commit mode: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Generates INSERT SQL statement
     */
    private String generateInsertSql(String tableName, TableMetadata metadata) {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(tableName).append(" (");
        
        // Add column names
        for (int i = 0; i < metadata.getColumns().size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(metadata.getColumns().get(i).getName());
        }
        
        sql.append(") VALUES (");
        
        // Add parameter placeholders
        for (int i = 0; i < metadata.getColumns().size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append("?");
        }
        
        sql.append(")");
        
        return sql.toString();
    }
    
    /**
     * Sets parameter values in the prepared statement
     */
    private void setParameters(PreparedStatement pstmt, TableMetadata metadata, ResultSet rs) throws SQLException {
        int paramIndex = 1;
        
        for (ColumnMetadata column : metadata.getColumns()) {
            Object value = rs.getObject(column.getName());
            
            if (value == null) {
                pstmt.setNull(paramIndex, getSqlType(column.getType()));
            } else {
                // Handle data transformation if needed
                Object transformedValue = transformValue(value, column);
                pstmt.setObject(paramIndex, transformedValue);
            }
            
            paramIndex++;
        }
    }
    
    /**
     * Transforms values based on data type and configuration
     */
    private Object transformValue(Object value, ColumnMetadata column) {
        // Handle data anonymization if enabled
        if (config.getMigration().getData().getAnonymizeData()) {
            return anonymizeValue(value, column);
        }
        
        // Handle data validation if enabled
        if (config.getMigration().getData().getValidateData()) {
            return validateValue(value, column);
        }
        
        return value;
    }
    
    /**
     * Anonymizes sensitive data
     */
    private Object anonymizeValue(Object value, ColumnMetadata column) {
        if (value == null) {
            return null;
        }
        
        String columnName = column.getName().toLowerCase();
        String valueStr = value.toString();
        
        // Check for common sensitive field patterns
        if (columnName.contains("email")) {
            return "user" + System.currentTimeMillis() % 1000 + "@example.com";
        } else if (columnName.contains("phone") || columnName.contains("tel")) {
            return "555-" + String.format("%04d", System.currentTimeMillis() % 10000);
        } else if (columnName.contains("password")) {
            return "********";
        } else if (columnName.contains("ssn") || columnName.contains("social")) {
            return "XXX-XX-" + String.format("%04d", System.currentTimeMillis() % 10000);
        } else if (columnName.contains("credit") || columnName.contains("card")) {
            return "****-****-****-" + String.format("%04d", System.currentTimeMillis() % 10000);
        }
        
        // Check custom anonymization rules
        if (config.getMigration().getData().getAnonymizationRules() != null) {
            String rule = config.getMigration().getData().getAnonymizationRules().get(columnName);
            if (rule != null) {
                return applyAnonymizationRule(value, rule);
            }
        }
        
        return value;
    }
    
    /**
     * Applies custom anonymization rules
     */
    private Object applyAnonymizationRule(Object value, String rule) {
        switch (rule.toLowerCase()) {
            case "hash":
                return String.valueOf(value.hashCode());
            case "random":
                return "RANDOM_" + System.currentTimeMillis() % 10000;
            case "null":
                return null;
            case "empty":
                return "";
            default:
                return value;
        }
    }
    
    /**
     * Validates data values
     */
    private Object validateValue(Object value, ColumnMetadata column) {
        if (value == null) {
            if (!column.isNullable()) {
                logger.warn("NULL value found for non-nullable column: {}", column.getName());
            }
            return null;
        }
        
        // Basic validation - in a real implementation, you might want more sophisticated validation
        String valueStr = value.toString();
        
        // Check length constraints
        if (column.getSize() > 0 && valueStr.length() > column.getSize()) {
            logger.warn("Value too long for column {}: {} (max: {})", 
                       column.getName(), valueStr.length(), column.getSize());
            return valueStr.substring(0, column.getSize());
        }
        
        return value;
    }
    
    /**
     * Gets SQL type for a column
     */
    private int getSqlType(String typeName) {
        String type = typeName.toUpperCase();
        
        switch (type) {
            case "VARCHAR":
            case "CHAR":
            case "TEXT":
            case "STRING":
                return Types.VARCHAR;
                
            case "INT":
            case "INTEGER":
            case "SMALLINT":
            case "TINYINT":
                return Types.INTEGER;
                
            case "BIGINT":
            case "LONG":
                return Types.BIGINT;
                
            case "DECIMAL":
            case "NUMERIC":
                return Types.DECIMAL;
                
            case "FLOAT":
            case "REAL":
                return Types.FLOAT;
                
            case "DOUBLE":
                return Types.DOUBLE;
                
            case "BOOLEAN":
            case "BOOL":
                return Types.BOOLEAN;
                
            case "DATE":
                return Types.DATE;
                
            case "TIME":
                return Types.TIME;
                
            case "TIMESTAMP":
            case "DATETIME":
                return Types.TIMESTAMP;
                
            case "BLOB":
            case "BINARY":
                return Types.BLOB;
                
            case "CLOB":
                return Types.CLOB;
                
            default:
                return Types.VARCHAR;
        }
    }
    
    /**
     * Gets the target database connection
     */
    private Connection getTargetConnection() throws SQLException {
        if (!targetConnector.isConnected()) {
            targetConnector.connect();
        }
        return targetConnector.getConnection();
    }
} 