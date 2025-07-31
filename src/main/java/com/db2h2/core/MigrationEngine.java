package com.db2h2.core;

import com.db2h2.config.MigrationConfig;
import com.db2h2.connectors.DatabaseConnector;
import com.db2h2.connectors.DatabaseConnectorFactory;
import com.db2h2.utils.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Main migration engine that orchestrates the migration process
 */
public class MigrationEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(MigrationEngine.class);
    
    private final MigrationConfig config;
    private final DatabaseConnector sourceConnector;
    private final DatabaseConnector targetConnector;
    private final SchemaMigrator schemaMigrator;
    private final DataMigrator dataMigrator;
    private final ProgressTracker progressTracker;
    
    public MigrationEngine(MigrationConfig config) {
        this.config = config;
        this.sourceConnector = DatabaseConnectorFactory.createConnector(config.getSource());
        this.targetConnector = DatabaseConnectorFactory.createConnector(config.getTarget());
        this.schemaMigrator = new SchemaMigrator(sourceConnector, targetConnector, config);
        this.dataMigrator = new DataMigrator(sourceConnector, targetConnector, config);
        this.progressTracker = new ProgressTracker();
    }
    
    /**
     * Executes the complete migration process
     */
    public MigrationResult migrate() {
        MigrationResult result = new MigrationResult();
        
        try {
            logger.info("Starting migration from {} to H2", config.getSource().getType());
            progressTracker.start();
            
            // Phase 1: Initialize connections
            initializeConnections();
            
            // Phase 2: Analyze source schema
            List<String> tableNames = analyzeSourceSchema();
            
            // Phase 3: Filter tables based on configuration
            List<String> filteredTables = filterTables(tableNames);
            
            // Phase 4: Migrate schema
            schemaMigrator.migrateSchema(filteredTables);
            
            // Phase 5: Migrate data (if enabled)
            if (config.getMigration().getPreserveData()) {
                dataMigrator.migrateData(filteredTables);
            }
            
            // Phase 6: Finalize migration
            finalizeMigration();
            
            progressTracker.complete();
            result.setSuccess(true);
            result.setMessage("Migration completed successfully");
            result.setTargetDatabase(config.getTarget().getFile());
            result.setTablesMigrated(filteredTables.size());
            result.setDuration(progressTracker.getDuration());
            
            logger.info("Migration completed successfully. {} tables migrated in {} ms", 
                       filteredTables.size(), progressTracker.getDuration());
            
        } catch (Exception e) {
            logger.error("Migration failed: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("Migration failed: " + e.getMessage());
            result.setError(e);
            
            if (config.getOutput().getExitOnError()) {
                System.exit(1);
            }
        } finally {
            cleanup();
        }
        
        return result;
    }
    
    /**
     * Initializes database connections
     */
    private void initializeConnections() throws SQLException {
        logger.info("Initializing database connections...");
        
        // Connect to source database
        sourceConnector.connect();
        logger.info("Connected to source database: {} {}", 
                   sourceConnector.getDatabaseType(), sourceConnector.getDatabaseVersion());
        
        // Connect to target database
        targetConnector.connect();
        logger.info("Connected to target database: H2");
    }
    
    /**
     * Analyzes the source database schema
     */
    private List<String> analyzeSourceSchema() throws SQLException {
        logger.info("Analyzing source database schema...");
        
        List<String> tableNames = sourceConnector.getTableNames();
        logger.info("Found {} tables in source database", tableNames.size());
        
        // Log table names for debugging
        for (String tableName : tableNames) {
            long rowCount = sourceConnector.getRowCount(tableName);
            logger.debug("Table: {} ({} rows)", tableName, rowCount);
        }
        
        return tableNames;
    }
    
    /**
     * Filters tables based on configuration
     */
    private List<String> filterTables(List<String> allTables) {
        List<String> tables = config.getMigration().getTables();
        List<String> excludeTables = config.getMigration().getExcludeTables();
        
        if (tables != null && !tables.isEmpty()) {
            // If specific tables are specified, only include those
            if (tables.contains("*")) {
                // Include all tables except excluded ones
                return allTables.stream()
                    .filter(tableName -> !isExcluded(tableName, excludeTables))
                    .toList();
            } else {
                // Include only specified tables
                return tables.stream()
                    .filter(allTables::contains)
                    .filter(tableName -> !isExcluded(tableName, excludeTables))
                    .toList();
            }
        } else {
            // Include all tables except excluded ones
            return allTables.stream()
                .filter(tableName -> !isExcluded(tableName, excludeTables))
                .toList();
        }
    }
    
    /**
     * Checks if a table should be excluded
     */
    private boolean isExcluded(String tableName, List<String> excludePatterns) {
        if (excludePatterns == null || excludePatterns.isEmpty()) {
            return false;
        }
        
        return excludePatterns.stream().anyMatch(pattern -> {
            if (pattern.contains("*")) {
                // Handle wildcard patterns
                String regex = pattern.replace("*", ".*");
                return tableName.matches(regex);
            } else {
                return tableName.equalsIgnoreCase(pattern);
            }
        });
    }
    
    /**
     * Finalizes the migration process
     */
    private void finalizeMigration() throws SQLException {
        logger.info("Finalizing migration...");
        
        // Update sequences if needed
        if (config.getMigration().getConstraints().getPreserveSequences()) {
            schemaMigrator.updateSequences();
        }
        
        // Commit any pending transactions
        if (targetConnector.isConnected()) {
            targetConnector.executeUpdate("COMMIT");
        }
        
        logger.info("Migration finalized successfully");
    }
    
    /**
     * Cleans up resources
     */
    private void cleanup() {
        logger.info("Cleaning up resources...");
        
        try {
            if (sourceConnector != null) {
                sourceConnector.disconnect();
            }
        } catch (Exception e) {
            logger.warn("Error disconnecting from source database: {}", e.getMessage());
        }
        
        try {
            if (targetConnector != null) {
                targetConnector.disconnect();
            }
        } catch (Exception e) {
            logger.warn("Error disconnecting from target database: {}", e.getMessage());
        }
    }
    
    /**
     * Result of the migration process
     */
    public static class MigrationResult {
        private boolean success;
        private String message;
        private String targetDatabase;
        private int tablesMigrated;
        private long duration;
        private Exception error;
        
        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getTargetDatabase() { return targetDatabase; }
        public void setTargetDatabase(String targetDatabase) { this.targetDatabase = targetDatabase; }
        
        public int getTablesMigrated() { return tablesMigrated; }
        public void setTablesMigrated(int tablesMigrated) { this.tablesMigrated = tablesMigrated; }
        
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
        
        public Exception getError() { return error; }
        public void setError(Exception error) { this.error = error; }
        
        @Override
        public String toString() {
            return "MigrationResult{" +
                    "success=" + success +
                    ", message='" + message + '\'' +
                    ", targetDatabase='" + targetDatabase + '\'' +
                    ", tablesMigrated=" + tablesMigrated +
                    ", duration=" + duration +
                    '}';
        }
    }
} 