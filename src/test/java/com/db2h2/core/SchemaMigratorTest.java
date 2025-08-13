package com.db2h2.core;

import com.db2h2.config.MigrationConfig;
import com.db2h2.connectors.DatabaseConnector;
import com.db2h2.schema.ColumnMetadata;
import com.db2h2.schema.TableMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchemaMigratorTest {

    @Mock
    private DatabaseConnector sourceConnector;
    
    @Mock
    private DatabaseConnector targetConnector;
    
    private MigrationConfig config;
    private SchemaMigrator schemaMigrator;

    @BeforeEach
    void setUp() {
        config = new MigrationConfig();
        schemaMigrator = new SchemaMigrator(sourceConnector, targetConnector, config);
    }

    @Test
    void testMapDataTypeWithNormalVarcharSize() {
        // Test normal VARCHAR sizes
        assertEquals("VARCHAR(255)", schemaMigrator.mapDataType("VARCHAR", 255));
        assertEquals("VARCHAR(1000)", schemaMigrator.mapDataType("VARCHAR", 1000));
        assertEquals("VARCHAR(50000)", schemaMigrator.mapDataType("VARCHAR", 50000));
    }

    @Test
    void testMapDataTypeWithUnlimitedSize() {
        // Test PostgreSQL unlimited VARCHAR size (2147483647)
        assertEquals("CLOB", schemaMigrator.mapDataType("VARCHAR", 2147483647));
        assertEquals("CLOB", schemaMigrator.mapDataType("TEXT", 2147483647));
    }

    @Test
    void testMapDataTypeWithLargeSize() {
        // Test sizes above threshold (default 1000000)
        assertEquals("CLOB", schemaMigrator.mapDataType("VARCHAR", 1500000));
        assertEquals("CLOB", schemaMigrator.mapDataType("TEXT", 2000000));
    }

    @Test
    void testMapDataTypeWithMySQLTextSizes() {
        // Test MySQL specific text sizes
        assertEquals("CLOB", schemaMigrator.mapDataType("TEXT", 65535));        // MySQL TEXT
        assertEquals("VARCHAR", schemaMigrator.mapDataType("MEDIUMTEXT", 16777215)); // MySQL MEDIUMTEXT (unknown type)
        assertEquals("VARCHAR", schemaMigrator.mapDataType("LONGTEXT", 2147483647)); // MySQL LONGTEXT (unknown type)
    }

    @Test
    void testMapDataTypeWithZeroSize() {
        // Test zero or negative sizes
        assertEquals("CLOB", schemaMigrator.mapDataType("VARCHAR", 0));
        assertEquals("CLOB", schemaMigrator.mapDataType("TEXT", -1));
    }

    @Test
    void testMapDataTypeWithCustomThreshold() {
        // Test with custom threshold
        config.getMigration().setMaxVarcharSizeThreshold(500000);
        
        // Should be VARCHAR below threshold
        assertEquals("VARCHAR(250000)", schemaMigrator.mapDataType("VARCHAR", 250000));
        
        // Should be CLOB above threshold
        assertEquals("CLOB", schemaMigrator.mapDataType("VARCHAR", 750000));
    }

    @Test
    void testMapDataTypeWithNonTextTypes() {
        // Test that non-text types are not affected
        assertEquals("INT", schemaMigrator.mapDataType("INT", 2147483647));
        assertEquals("BIGINT", schemaMigrator.mapDataType("BIGINT", 2147483647));
        assertEquals("DECIMAL", schemaMigrator.mapDataType("DECIMAL", 2147483647));
    }

    @Test
    void testMapDataTypeWithUnknownType() {
        // Test unknown types fall back to VARCHAR
        assertEquals("VARCHAR", schemaMigrator.mapDataType("UNKNOWN_TYPE", 100));
    }

    @Test
    void testMapDataTypeWithNullSize() {
        // Test edge case with null size (should be treated as unlimited)
        assertEquals("CLOB", schemaMigrator.mapDataType("VARCHAR", 0));
    }

    @Test
    void testMapDataTypeWithEdgeCaseSizes() {
        // Test edge cases around the threshold
        config.getMigration().setMaxVarcharSizeThreshold(1000000);
        
        // Exactly at threshold
        assertEquals("VARCHAR(1000000)", schemaMigrator.mapDataType("VARCHAR", 1000000));
        
        // Just above threshold
        assertEquals("CLOB", schemaMigrator.mapDataType("VARCHAR", 1000001));
        
        // Just below threshold
        assertEquals("VARCHAR(999999)", schemaMigrator.mapDataType("VARCHAR", 999999));
    }
}
