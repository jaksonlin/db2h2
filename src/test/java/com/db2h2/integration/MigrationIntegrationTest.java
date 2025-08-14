package com.db2h2.integration;

import com.db2h2.config.DatabaseConfig;
import com.db2h2.config.MigrationConfig;
import com.db2h2.core.MigrationEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.sql.*;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the complete migration process
 */
public class MigrationIntegrationTest {
    
    @TempDir
    Path tempDir;
    
    private MigrationConfig config;
    private String h2SourcePath;
    private String h2TargetPath;
    
    @BeforeEach
    void setUp() {
        h2SourcePath = tempDir.resolve("source-test-db").toString();
        h2TargetPath = tempDir.resolve("target-test-db").toString();
        
        config = createTestConfig();
        setupSourceDatabase();
    }
    
    @Test
    void testCompleteH2ToH2Migration() throws Exception {
        // Execute migration
        MigrationEngine engine = new MigrationEngine(config);
        MigrationEngine.MigrationResult result = engine.migrate();
        
        // Verify migration success
        assertTrue(result.isSuccess(), "Migration should succeed");
        assertEquals(2, result.getTablesMigrated(), "Should migrate 2 tables");
        assertTrue(result.getDuration() > 0, "Duration should be positive");
        
        // Verify target database structure and data
        verifyTargetDatabase();
    }
    
    @Test
    void testMigrationWithTableFiltering() throws Exception {
        // Configure to migrate only users table
        config.getMigration().setTables(Arrays.asList("users"));
        
        MigrationEngine engine = new MigrationEngine(config);
        MigrationEngine.MigrationResult result = engine.migrate();
        
        assertTrue(result.isSuccess());
        assertEquals(1, result.getTablesMigrated());
        
        // Verify only users table exists in target
        try (Connection conn = DriverManager.getConnection("jdbc:h2:" + h2TargetPath)) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            // Check users table exists
            try (ResultSet rs = metaData.getTables(null, null, "USERS", null)) {
                assertTrue(rs.next(), "Users table should exist");
            }
            
            // Check orders table doesn't exist
            try (ResultSet rs = metaData.getTables(null, null, "ORDERS", null)) {
                assertFalse(rs.next(), "Orders table should not exist");
            }
        }
    }
    
    @Test
    void testMigrationWithDataSampling() throws Exception {
        // Configure data sampling
        config.getMigration().getData().setSampleData(true);
        config.getMigration().getData().setSamplePercentage(50);
        
        MigrationEngine engine = new MigrationEngine(config);
        MigrationEngine.MigrationResult result = engine.migrate();
        
        assertTrue(result.isSuccess());
        
        // Verify reduced data in target (approximately 50%)
        try (Connection conn = DriverManager.getConnection("jdbc:h2:" + h2TargetPath)) {
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
                rs.next();
                int targetCount = rs.getInt(1);
                
                // Should have less data due to sampling (allowing some variance)
                assertTrue(targetCount <= 5, "Should have sampled data (≤5 rows)");
                assertTrue(targetCount > 0, "Should have some data");
            }
        }
    }
    
    @Test
    void testMigrationWithMaxRowsLimit() throws Exception {
        // Configure max rows limit
        config.getMigration().getData().setMaxRows(3);
        
        MigrationEngine engine = new MigrationEngine(config);
        MigrationEngine.MigrationResult result = engine.migrate();
        
        assertTrue(result.isSuccess());
        
        // Verify limited data in target
        try (Connection conn = DriverManager.getConnection("jdbc:h2:" + h2TargetPath)) {
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
                rs.next();
                int targetCount = rs.getInt(1);
                
                assertEquals(3, targetCount, "Should have exactly 3 rows due to limit");
            }
        }
    }
    
    @Test
    void testMigrationWithInvalidConfig() {
        // Test with invalid source database
        config.getSource().setHost("invalid-host");
        config.getSource().setDatabase("invalid-db");
        
        MigrationEngine engine = new MigrationEngine(config);
        MigrationEngine.MigrationResult result = engine.migrate();
        
        assertFalse(result.isSuccess(), "Migration should fail with invalid config");
        assertNotNull(result.getMessage(), "Should have error message");
        assertNotNull(result.getError(), "Should have error details");
    }
    
    private MigrationConfig createTestConfig() {
        MigrationConfig config = new MigrationConfig();
        
        // Source configuration (H2 for testing)
        DatabaseConfig source = new DatabaseConfig();
        source.setType("h2");
        source.setFile(h2SourcePath);
        source.setMode("file");
        config.setSource(source);
        
        // Target configuration
        DatabaseConfig target = new DatabaseConfig();
        target.setType("h2");
        target.setFile(h2TargetPath);
        target.setMode("file");
        config.setTarget(target);
        
        // Migration settings
        config.getMigration().setTables(Arrays.asList("*"));
        config.getMigration().setBatchSize(100);
        config.getMigration().setPreserveData(true);
        
        // Output settings
        config.getOutput().setExitOnError(false);
        config.getOutput().setLogLevel("DEBUG");
        
        return config;
    }
    
    private void setupSourceDatabase() {
        try (Connection conn = DriverManager.getConnection("jdbc:h2:" + h2SourcePath)) {
            try (Statement stmt = conn.createStatement()) {
                // Create users table
                stmt.execute("""
                    CREATE TABLE users (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        username VARCHAR(50) NOT NULL UNIQUE,
                        email VARCHAR(100) NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        active BOOLEAN DEFAULT TRUE
                    )
                """);
                
                // Create orders table
                stmt.execute("""
                    CREATE TABLE orders (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        user_id INT NOT NULL,
                        total DECIMAL(10,2) NOT NULL,
                        status VARCHAR(20) DEFAULT 'pending',
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (user_id) REFERENCES users(id)
                    )
                """);
                
                // Insert test data
                stmt.execute("""
                    INSERT INTO users (username, email) VALUES 
                    ('john_doe', 'john@example.com'),
                    ('jane_smith', 'jane@example.com'),
                    ('bob_wilson', 'bob@example.com'),
                    ('alice_brown', 'alice@example.com'),
                    ('charlie_davis', 'charlie@example.com')
                """);
                
                stmt.execute("""
                    INSERT INTO orders (user_id, total, status) VALUES 
                    (1, 99.99, 'completed'),
                    (1, 149.50, 'pending'),
                    (2, 75.25, 'completed'),
                    (3, 200.00, 'shipped'),
                    (4, 50.00, 'pending')
                """);
                
                // Create indexes
                stmt.execute("CREATE INDEX idx_users_email ON users(email)");
                stmt.execute("CREATE INDEX idx_orders_user_id ON orders(user_id)");
                stmt.execute("CREATE INDEX idx_orders_status ON orders(status)");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to setup source database", e);
        }
    }
    
    private void verifyTargetDatabase() throws SQLException {
        try (Connection conn = DriverManager.getConnection("jdbc:h2:" + h2TargetPath)) {
            // Verify table structure
            DatabaseMetaData metaData = conn.getMetaData();
            
            // Check users table
            try (ResultSet rs = metaData.getTables(null, null, "USERS", null)) {
                assertTrue(rs.next(), "Users table should exist");
            }
            
            // Check orders table
            try (ResultSet rs = metaData.getTables(null, null, "ORDERS", null)) {
                assertTrue(rs.next(), "Orders table should exist");
            }
            
            // Verify data
            try (Statement stmt = conn.createStatement()) {
                // Check users data
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
                rs.next();
                assertEquals(5, rs.getInt(1), "Should have 5 users");
                
                // Check orders data
                rs = stmt.executeQuery("SELECT COUNT(*) FROM orders");
                rs.next();
                assertEquals(5, rs.getInt(1), "Should have 5 orders");
                
                // Verify foreign key relationship
                rs = stmt.executeQuery("""
                    SELECT COUNT(*) FROM orders o 
                    JOIN users u ON o.user_id = u.id
                """);
                rs.next();
                assertEquals(5, rs.getInt(1), "All orders should have valid user references");
            }
            
            // Verify indexes
            try (ResultSet rs = metaData.getIndexInfo(null, null, "USERS", false, false)) {
                boolean hasEmailIndex = false;
                while (rs.next()) {
                    String indexName = rs.getString("INDEX_NAME");
                    if ("IDX_USERS_EMAIL".equals(indexName)) {
                        hasEmailIndex = true;
                        break;
                    }
                }
                assertTrue(hasEmailIndex, "Email index should exist");
            }
        }
    }
}