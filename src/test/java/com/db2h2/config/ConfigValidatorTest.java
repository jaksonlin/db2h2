package com.db2h2.config;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for configuration validation
 */
public class ConfigValidatorTest {
    
    private MigrationConfig validConfig;
    
    @Before
    public void setUp() {
        validConfig = createValidConfig();
    }
    
    @Test
    public void testValidConfiguration() {
        ConfigValidator.ValidationResult result = ConfigValidator.validate(validConfig);
        
        assertTrue("Valid configuration should pass validation", result.isValid());
        assertFalse("Should have no errors", result.hasErrors());
    }
    
    @Test
    public void testNullConfiguration() {
        ConfigValidator.ValidationResult result = ConfigValidator.validate(null);
        
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().contains("Configuration cannot be null"));
    }
    
    @Test
    public void testMissingSourceDatabase() {
        validConfig.setSource(null);
        
        ConfigValidator.ValidationResult result = ConfigValidator.validate(validConfig);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Source database configuration is required"));
    }
    
    @Test
    public void testMissingTargetDatabase() {
        validConfig.setTarget(null);
        
        ConfigValidator.ValidationResult result = ConfigValidator.validate(validConfig);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Target database configuration is required"));
    }
    
    @Test
    public void testInvalidSourceDatabaseType() {
        validConfig.getSource().setType("unsupported_db");
        
        ConfigValidator.ValidationResult result = ConfigValidator.validate(validConfig);
        
        assertFalse(result.isValid());
        boolean hasError = false;
        for (String error : result.getErrors()) {
            if (error.contains("Unsupported source database type")) {
                hasError = true;
                break;
            }
        }
        assertTrue(hasError);
    }
    
    @Test
    public void testMissingSourceHost() {
        validConfig.getSource().setHost(null);
        
        ConfigValidator.ValidationResult result = ConfigValidator.validate(validConfig);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Source database host is required"));
    }
    
    @Test
    public void testMissingSourceDatabaseName() {
        validConfig.getSource().setDatabase(null);
        
        ConfigValidator.ValidationResult result = ConfigValidator.validate(validConfig);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Source database name is required"));
    }
    
    @Test
    public void testMissingSourceUsername() {
        validConfig.getSource().setUsername(null);
        
        ConfigValidator.ValidationResult result = ConfigValidator.validate(validConfig);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Source database username is required"));
    }
    
    @Test
    public void testInvalidSourcePort() {
        validConfig.getSource().setPort(70000); // Invalid port
        
        ConfigValidator.ValidationResult result = ConfigValidator.validate(validConfig);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Source database port must be between 1 and 65535"));
    }
    
    @Test
    public void testNonH2TargetDatabase() {
        validConfig.getTarget().setType("mysql");
        
        ConfigValidator.ValidationResult result = ConfigValidator.validate(validConfig);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Target database must be H2"));
    }
    
    @Test
    public void testMissingH2FilePath() {
        validConfig.getTarget().setFile(null);
        
        ConfigValidator.ValidationResult result = ConfigValidator.validate(validConfig);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("H2 database file path is required"));
    }
    
    @Test
    public void testInvalidH2Mode() {
        validConfig.getTarget().setMode("invalid_mode");
        
        ConfigValidator.ValidationResult result = ConfigValidator.validate(validConfig);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("H2 mode must be 'file' or 'memory'"));
    }
    
    @Test
    public void testInvalidBatchSize() {
        validConfig.getMigration().setBatchSize(-1);
        
        ConfigValidator.ValidationResult result = ConfigValidator.validate(validConfig);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Batch size must be greater than 0"));
    }
    
    @Test
    public void testInvalidMaxRows() {
        validConfig.getMigration().getData().setMaxRows(-1);
        
        ConfigValidator.ValidationResult result = ConfigValidator.validate(validConfig);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Max rows must be greater than 0"));
    }
    
    @Test
    public void testInvalidSamplePercentage() {
        validConfig.getMigration().getData().setSamplePercentage(150); // > 100
        
        ConfigValidator.ValidationResult result = ConfigValidator.validate(validConfig);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Sample percentage must be between 1 and 100"));
    }
    
    @Test
    public void testSameSourceAndTargetH2File() {
        // Set both source and target to H2 with same file
        validConfig.getSource().setType("h2");
        validConfig.getSource().setFile("/tmp/test.h2.db");
        validConfig.getTarget().setFile("/tmp/test.h2.db");
        
        ConfigValidator.ValidationResult result = ConfigValidator.validate(validConfig);
        
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Source and target cannot be the same H2 database file"));
    }
    
    @Test
    public void testWarningForSamplingAndMaxRows() {
        validConfig.getMigration().getData().setSampleData(true);
        validConfig.getMigration().getData().setMaxRows(1000);
        
        ConfigValidator.ValidationResult result = ConfigValidator.validate(validConfig);
        
        assertTrue(result.isValid()); // Should still be valid
        assertTrue(result.hasWarnings());
        boolean hasWarning = false;
        for (String warning : result.getWarnings()) {
            if (warning.contains("Both data sampling and max rows limit are configured")) {
                hasWarning = true;
                break;
            }
        }
        assertTrue(hasWarning);
    }
    
    @Test
    public void testPostgreSQLDefaultPort() {
        validConfig.getSource().setType("postgresql");
        validConfig.getSource().setPort(null); // No port specified
        
        ConfigValidator.ValidationResult result = ConfigValidator.validate(validConfig);
        
        assertTrue(result.isValid());
        assertEquals(5432, (int) validConfig.getSource().getPort()); // Should be set to default
    }
    
    @Test
    public void testMySQLDefaultPort() {
        validConfig.getSource().setType("mysql");
        validConfig.getSource().setPort(null);
        
        ConfigValidator.ValidationResult result = ConfigValidator.validate(validConfig);
        
        assertTrue(result.isValid());
        assertEquals(3306, (int) validConfig.getSource().getPort());
    }
    
    @Test
    public void testOracleDefaultPort() {
        validConfig.getSource().setType("oracle");
        validConfig.getSource().setPort(null);
        
        ConfigValidator.ValidationResult result = ConfigValidator.validate(validConfig);
        
        assertTrue(result.isValid());
        assertEquals(1521, (int) validConfig.getSource().getPort());
    }
    
    @Test
    public void testSqlServerDefaultPort() {
        validConfig.getSource().setType("sqlserver");
        validConfig.getSource().setPort(null);
        
        ConfigValidator.ValidationResult result = ConfigValidator.validate(validConfig);
        
        assertTrue(result.isValid());
        assertEquals(1433, (int) validConfig.getSource().getPort());
    }
    
    @Test
    public void testAnonymizationWarning() {
        validConfig.getMigration().getData().setAnonymizeData(true);
        validConfig.getMigration().getData().setAnonymizationRules(null);
        
        ConfigValidator.ValidationResult result = ConfigValidator.validate(validConfig);
        
        assertTrue(result.isValid());
        assertTrue(result.hasWarnings());
        boolean hasWarning = false;
        for (String warning : result.getWarnings()) {
            if (warning.contains("Data anonymization is enabled but no anonymization rules are defined")) {
                hasWarning = true;
                break;
            }
        }
        assertTrue(hasWarning);
    }
    
    private MigrationConfig createValidConfig() {
        MigrationConfig config = new MigrationConfig();
        
        // Source database
        DatabaseConfig source = new DatabaseConfig();
        source.setType("postgresql");
        source.setHost("localhost");
        source.setPort(5432);
        source.setDatabase("source_db");
        source.setUsername("user");
        source.setPassword("password");
        config.setSource(source);
        
        // Target database
        DatabaseConfig target = new DatabaseConfig();
        target.setType("h2");
        target.setFile("./target/test-db.h2.db");
        target.setMode("file");
        config.setTarget(target);
        
        // Migration settings
        config.getMigration().setBatchSize(1000);
        config.getMigration().setPreserveData(true);
        config.getMigration().getData().setMaxRows(10000);
        config.getMigration().getData().setSamplePercentage(50);
        
        return config;
    }
}