package com.db2h2.core;

import com.db2h2.config.MigrationConfig;
import com.db2h2.connectors.DatabaseConnector;
import com.db2h2.schema.ColumnMetadata;
import com.db2h2.schema.TableMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SchemaMigratorTest {

    @Mock
    private DatabaseConnector sourceConnector;
    
    @Mock
    private DatabaseConnector targetConnector;
    
    @Mock
    private MigrationConfig config;
    
    @Mock
    private MigrationConfig.MigrationSettings migrationSettings;
    
    private SchemaMigrator schemaMigrator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        when(sourceConnector.getDatabaseType()).thenReturn("postgresql");
        when(config.getMigration()).thenReturn(migrationSettings);
        when(migrationSettings.getFunctionMappings()).thenReturn(null);
        
        schemaMigrator = new SchemaMigrator(sourceConnector, targetConnector, config);
    }

    @Test
    void testTranslateDefaultValue_GenRandomUuid() {
        // Test that gen_random_uuid() is translated to RANDOM_UUID()
        String result = schemaMigrator.translateDefaultValue("gen_random_uuid()");
        assertEquals("RANDOM_UUID()", result);
    }

    @Test
    void testTranslateDefaultValue_TypeCasting() {
        // Test that PostgreSQL type casting is removed
        String result = schemaMigrator.translateDefaultValue("'{}'::jsonb");
        assertEquals("'{}'", result);
    }

    @Test
    void testTranslateDefaultValue_ArraySyntax() {
        // Test that PostgreSQL array syntax is removed
        String result = schemaMigrator.translateDefaultValue("'{}'::text[]");
        assertEquals("'{}'", result);
    }

    @Test
    void testTranslateDefaultValue_CharacterVarying() {
        // Test that character varying type casting is removed
        String result = schemaMigrator.translateDefaultValue("'member'::character varying");
        assertEquals("'member'", result);
    }

    @Test
    void testTranslateDefaultValue_CurrentTimestamp() {
        // Test that CURRENT_TIMESTAMP is preserved
        String result = schemaMigrator.translateDefaultValue("CURRENT_TIMESTAMP");
        assertEquals("CURRENT_TIMESTAMP", result);
    }

    @Test
    void testTranslateDefaultValue_ComplexExpression() {
        // Test a complex expression with multiple PostgreSQL features
        String result = schemaMigrator.translateDefaultValue("'{}'::jsonb DEFAULT gen_random_uuid()");
        assertEquals("'{}' DEFAULT RANDOM_UUID()", result);
    }

    @Test
    void testTranslateDefaultValue_Null() {
        // Test that null is handled correctly
        String result = schemaMigrator.translateDefaultValue(null);
        assertNull(result);
    }

    @Test
    void testTranslateDefaultValue_NoChanges() {
        // Test that values without PostgreSQL features are unchanged
        String result = schemaMigrator.translateDefaultValue("'simple string'");
        assertEquals("'simple string'", result);
    }
}
