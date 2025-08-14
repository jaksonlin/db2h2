package com.db2h2.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates migration configuration for consistency and completeness
 */
public class ConfigValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigValidator.class);
    
    /**
     * Validates the complete migration configuration
     */
    public static ValidationResult validate(MigrationConfig config) {
        ValidationResult result = new ValidationResult();
        
        // Basic validation
        validateBasicConfig(config, result);
        
        // Source database validation
        validateSourceDatabase(config.getSource(), result);
        
        // Target database validation
        validateTargetDatabase(config.getTarget(), result);
        
        // Migration settings validation
        validateMigrationSettings(config.getMigration(), result);
        
        // Cross-validation
        validateCrossReferences(config, result);
        
        if (result.hasErrors()) {
            logger.error("Configuration validation failed with {} errors", result.getErrors().size());
            for (String error : result.getErrors()) {
                logger.error("  - {}", error);
            }
        } else {
            logger.info("Configuration validation passed");
        }
        
        return result;
    }
    
    /**
     * Validates basic configuration structure
     */
    private static void validateBasicConfig(MigrationConfig config, ValidationResult result) {
        if (config == null) {
            result.addError("Configuration cannot be null");
            return;
        }
        
        if (config.getSource() == null) {
            result.addError("Source database configuration is required");
        }
        
        if (config.getTarget() == null) {
            result.addError("Target database configuration is required");
        }
        
        if (config.getMigration() == null) {
            result.addError("Migration settings are required");
        }
        
        if (config.getOutput() == null) {
            result.addError("Output settings are required");
        }
    }
    
    /**
     * Validates source database configuration
     */
    private static void validateSourceDatabase(DatabaseConfig source, ValidationResult result) {
        if (source == null) {
            return; // Already handled in basic validation
        }
        
        // Database type validation
        if (source.getType() == null || source.getType().trim().isEmpty()) {
            result.addError("Source database type is required");
        } else {
            String type = source.getType().toLowerCase();
            if (!isValidDatabaseType(type)) {
                result.addError("Unsupported source database type: " + source.getType());
            }
        }
        
        // Connection parameters validation
        if (source.getHost() == null || source.getHost().trim().isEmpty()) {
            result.addError("Source database host is required");
        }
        
        if (source.getDatabase() == null || source.getDatabase().trim().isEmpty()) {
            result.addError("Source database name is required");
        }
        
        if (source.getUsername() == null || source.getUsername().trim().isEmpty()) {
            result.addError("Source database username is required");
        }
        
        // Port validation
        if (source.getPort() != null && (source.getPort() < 1 || source.getPort() > 65535)) {
            result.addError("Source database port must be between 1 and 65535");
        }
        
        // Database-specific validation
        validateDatabaseSpecificConfig(source, result, "source");
    }
    
    /**
     * Validates target database configuration
     */
    private static void validateTargetDatabase(DatabaseConfig target, ValidationResult result) {
        if (target == null) {
            return; // Already handled in basic validation
        }
        
        // Target must be H2
        if (target.getType() == null || !"h2".equalsIgnoreCase(target.getType())) {
            result.addError("Target database must be H2");
        }
        
        // H2 file path validation
        if (target.getFile() == null || target.getFile().trim().isEmpty()) {
            result.addError("H2 database file path is required");
        } else {
            // Validate file path is writable
            File targetFile = new File(target.getFile());
            File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    result.addWarning("Cannot create target directory: " + parentDir.getAbsolutePath());
                }
            }
        }
        
        // H2 mode validation
        if (target.getMode() != null) {
            String mode = target.getMode().toLowerCase();
            if (!"file".equals(mode) && !"memory".equals(mode)) {
                result.addError("H2 mode must be 'file' or 'memory'");
            }
        }
    }
    
    /**
     * Validates migration settings
     */
    private static void validateMigrationSettings(MigrationConfig.MigrationSettings migration, ValidationResult result) {
        if (migration == null) {
            return; // Already handled in basic validation
        }
        
        // Batch size validation
        if (migration.getBatchSize() != null && migration.getBatchSize() <= 0) {
            result.addError("Batch size must be greater than 0");
        }
        
        // Max VARCHAR size threshold validation
        if (migration.getMaxVarcharSizeThreshold() != null && migration.getMaxVarcharSizeThreshold() <= 0) {
            result.addError("Max VARCHAR size threshold must be greater than 0");
        }
        
        // Data settings validation
        if (migration.getData() != null) {
            validateDataSettings(migration.getData(), result);
        }
        
        // Constraint settings validation
        if (migration.getConstraints() != null) {
            validateConstraintSettings(migration.getConstraints(), result);
        }
    }
    
    /**
     * Validates data migration settings
     */
    private static void validateDataSettings(MigrationConfig.DataSettings data, ValidationResult result) {
        // Max rows validation
        if (data.getMaxRows() != null && data.getMaxRows() <= 0) {
            result.addError("Max rows must be greater than 0");
        }
        
        // Sample percentage validation
        if (data.getSamplePercentage() != null) {
            int percentage = data.getSamplePercentage();
            if (percentage < 1 || percentage > 100) {
                result.addError("Sample percentage must be between 1 and 100");
            }
        }
        
        // Anonymization validation
        if (data.getAnonymizeData() != null && data.getAnonymizeData()) {
            if (data.getAnonymizationRules() == null || data.getAnonymizationRules().isEmpty()) {
                result.addWarning("Data anonymization is enabled but no anonymization rules are defined");
            }
        }
    }
    
    /**
     * Validates constraint settings
     */
    private static void validateConstraintSettings(MigrationConfig.ConstraintSettings constraints, ValidationResult result) {
        // No specific validation needed for boolean flags currently
        // This method is here for future constraint validation logic
    }
    
    /**
     * Validates cross-references between different configuration sections
     */
    private static void validateCrossReferences(MigrationConfig config, ValidationResult result) {
        // Check if source and target are the same (which doesn't make sense)
        if (config.getSource() != null && config.getTarget() != null) {
            if ("h2".equalsIgnoreCase(config.getSource().getType()) && 
                "h2".equalsIgnoreCase(config.getTarget().getType())) {
                
                if (config.getSource().getFile() != null && 
                    config.getSource().getFile().equals(config.getTarget().getFile())) {
                    result.addError("Source and target cannot be the same H2 database file");
                }
            }
        }
        
        // Check if sampling and max rows are both configured
        if (config.getMigration() != null && config.getMigration().getData() != null) {
            MigrationConfig.DataSettings data = config.getMigration().getData();
            if (data.getSampleData() != null && data.getSampleData() && 
                data.getMaxRows() != null && data.getMaxRows() > 0) {
                result.addWarning("Both data sampling and max rows limit are configured - max rows will take precedence");
            }
        }
    }
    
    /**
     * Validates database-specific configuration
     */
    private static void validateDatabaseSpecificConfig(DatabaseConfig config, ValidationResult result, String prefix) {
        String type = config.getType().toLowerCase();
        
        switch (type) {
            case "postgresql":
            case "postgres":
                validatePostgreSQLConfig(config, result, prefix);
                break;
            case "mysql":
                validateMySQLConfig(config, result, prefix);
                break;
            case "oracle":
                validateOracleConfig(config, result, prefix);
                break;
            case "sqlserver":
            case "mssql":
                validateSqlServerConfig(config, result, prefix);
                break;
            case "h2":
                validateH2Config(config, result, prefix);
                break;
        }
    }
    
    /**
     * Validates PostgreSQL-specific configuration
     */
    private static void validatePostgreSQLConfig(DatabaseConfig config, ValidationResult result, String prefix) {
        // Default port check
        if (config.getPort() == null) {
            config.setPort(5432);
            result.addInfo(prefix + " PostgreSQL port not specified, using default: 5432");
        }
    }
    
    /**
     * Validates MySQL-specific configuration
     */
    private static void validateMySQLConfig(DatabaseConfig config, ValidationResult result, String prefix) {
        // Default port check
        if (config.getPort() == null) {
            config.setPort(3306);
            result.addInfo(prefix + " MySQL port not specified, using default: 3306");
        }
    }
    
    /**
     * Validates Oracle-specific configuration
     */
    private static void validateOracleConfig(DatabaseConfig config, ValidationResult result, String prefix) {
        // Default port check
        if (config.getPort() == null) {
            config.setPort(1521);
            result.addInfo(prefix + " Oracle port not specified, using default: 1521");
        }
    }
    
    /**
     * Validates SQL Server-specific configuration
     */
    private static void validateSqlServerConfig(DatabaseConfig config, ValidationResult result, String prefix) {
        // Default port check
        if (config.getPort() == null) {
            config.setPort(1433);
            result.addInfo(prefix + " SQL Server port not specified, using default: 1433");
        }
    }
    
    /**
     * Validates H2-specific configuration
     */
    private static void validateH2Config(DatabaseConfig config, ValidationResult result, String prefix) {
        // H2 doesn't typically use host/port for file mode
        if ("file".equalsIgnoreCase(config.getMode()) && config.getHost() != null) {
            result.addWarning(prefix + " H2 in file mode doesn't require host parameter");
        }
    }
    
    /**
     * Checks if the database type is supported
     */
    private static boolean isValidDatabaseType(String type) {
        return "postgresql".equals(type) || "postgres".equals(type) ||
               "mysql".equals(type) ||
               "oracle".equals(type) ||
               "sqlserver".equals(type) || "mssql".equals(type) ||
               "h2".equals(type);
    }
    
    /**
     * Validation result container
     */
    public static class ValidationResult {
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private final List<String> info = new ArrayList<>();
        
        public void addError(String error) {
            errors.add(error);
        }
        
        public void addWarning(String warning) {
            warnings.add(warning);
        }
        
        public void addInfo(String info) {
            this.info.add(info);
        }
        
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
        
        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
        
        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }
        
        public List<String> getWarnings() {
            return new ArrayList<>(warnings);
        }
        
        public List<String> getInfo() {
            return new ArrayList<>(info);
        }
        
        public boolean isValid() {
            return !hasErrors();
        }
    }
}