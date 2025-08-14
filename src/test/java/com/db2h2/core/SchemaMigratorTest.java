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

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

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
    void testDisableAutoIncrementDuringMigration_WhenConfigured_ShouldProcessTables() throws SQLException {
        // Arrange
        config.getMigration().getConstraints().setDisableAutoIncrementDuringMigration(true);
        
        TableMetadata tableMetadata = new TableMetadata();
        tableMetadata.setTableName("test_table");
        
        ColumnMetadata autoIncrementColumn = new ColumnMetadata();
        autoIncrementColumn.setName("id");
        autoIncrementColumn.setAutoIncrement(true);
        
        ColumnMetadata regularColumn = new ColumnMetadata();
        regularColumn.setName("name");
        regularColumn.setAutoIncrement(false);
        
        tableMetadata.setColumns(Arrays.asList(autoIncrementColumn, regularColumn));
        
        when(sourceConnector.getTableNames()).thenReturn(Arrays.asList("test_table"));
        when(sourceConnector.getTableMetadata("test_table")).thenReturn(tableMetadata);

        // Act
        schemaMigrator.disableAutoIncrement();

        // Assert
        verify(sourceConnector).getTableNames();
        verify(sourceConnector).getTableMetadata("test_table");
    }

    @Test
    void testDisableAutoIncrementDuringMigration_WhenNotConfigured_ShouldSkip() throws SQLException {
        // Arrange
        config.getMigration().getConstraints().setDisableAutoIncrementDuringMigration(false);

        // Act
        schemaMigrator.disableAutoIncrement();

        // Assert
        verifyNoInteractions(sourceConnector);
    }

    @Test
    void testReEnableAutoIncrement_WhenConfigured_ShouldProcessTables() throws SQLException {
        // Arrange
        config.getMigration().getConstraints().setDisableAutoIncrementDuringMigration(true);
        
        TableMetadata tableMetadata = new TableMetadata();
        tableMetadata.setTableName("test_table");
        
        ColumnMetadata autoIncrementColumn = new ColumnMetadata();
        autoIncrementColumn.setName("id");
        autoIncrementColumn.setAutoIncrement(true);
        
        tableMetadata.setColumns(Arrays.asList(autoIncrementColumn));
        
        when(sourceConnector.getTableNames()).thenReturn(Arrays.asList("test_table"));
        when(sourceConnector.getTableMetadata("test_table")).thenReturn(tableMetadata);

        // Act
        schemaMigrator.reEnableAutoIncrement();

        // Assert
        verify(sourceConnector).getTableNames();
        verify(sourceConnector).getTableMetadata("test_table");
        verify(targetConnector).executeUpdate("ALTER TABLE test_table ALTER COLUMN id AUTO_INCREMENT");
    }

    @Test
    void testReEnableAutoIncrement_WhenNotConfigured_ShouldSkip() throws SQLException {
        // Arrange
        config.getMigration().getConstraints().setDisableAutoIncrementDuringMigration(false);

        // Act
        schemaMigrator.reEnableAutoIncrement();

        // Assert
        verifyNoInteractions(sourceConnector, targetConnector);
    }

    @Test
    void testUpdateSequences_WhenAutoIncrementDisabled_ShouldLogAndReturn() throws SQLException {
        // Arrange
        config.getMigration().getConstraints().setDisableAutoIncrementDuringMigration(true);

        // Act
        schemaMigrator.updateSequences();

        // Assert
        verifyNoInteractions(sourceConnector, targetConnector);
    }
}
