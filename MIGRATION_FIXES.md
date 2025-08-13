# Migration Fixes Documentation

## Issues Identified and Resolved

### 1. Missing Function Mapping System

**Problem**: The tool was failing because PostgreSQL-specific functions like `gen_random_uuid()` were being used directly in H2 CREATE TABLE statements, but H2 doesn't support these functions.

**Error Examples**:
```
Function "GEN_RANDOM_UUID" not found; SQL statement:
CREATE TABLE api_keys (id UUID NOT NULL DEFAULT gen_random_uuid(), ...)
```

**Solution**: Implemented a comprehensive function mapping system that translates database-specific functions to H2-compatible alternatives.

**Function Mappings Added**:
- `gen_random_uuid()` → `RANDOM_UUID()` (PostgreSQL to H2)
- `UUID()` → `RANDOM_UUID()` (MySQL to H2)
- `SYS_GUID()` → `RANDOM_UUID()` (Oracle to H2)
- `NEWID()` → `RANDOM_UUID()` (SQL Server to H2)
- `CURRENT_TIMESTAMP` → `CURRENT_TIMESTAMP` (preserved)
- `now()` → `CURRENT_TIMESTAMP` (PostgreSQL to H2)

### 2. PostgreSQL Type Casting Issues

**Problem**: PostgreSQL default values often include type casting (e.g., `'{}'::jsonb`, `'member'::character varying`) which H2 doesn't support.

**Error Examples**:
```
CREATE TABLE teams (..., settings VARCHAR DEFAULT '{}'::jsonb, ...)
CREATE TABLE users (..., role VARCHAR(50) NOT NULL DEFAULT 'member'::character varying, ...)
```

**Solution**: Added automatic type casting removal in the `translateDefaultValue` method.

**Patterns Handled**:
- `'{}'::jsonb` → `'{}'`
- `'member'::character varying` → `'member'`
- `'active'::character varying` → `'active'`
- `0::numeric` → `0`
- `true::boolean` → `true`

### 3. Duplicate Index and Constraint Errors

**Problem**: The migration was trying to create indexes and foreign key constraints that already existed, causing errors like:
```
Index "PATTERN_SHARES_PKEY" already exists
Index "TEAM_MEMBERS_PKEY" already exists
```

**Solution**: Added existence checks before creating indexes and foreign key constraints.

**Methods Added**:
- `indexExists()` - Checks if an index already exists
- `foreignKeyExists()` - Checks if a foreign key constraint already exists
- `tableExists()` - Checks if a table already exists

### 4. Table Recreation Strategy

**Problem**: When tables already exist, the migration would fail with constraint errors.

**Solution**: Implemented a drop-and-recreate strategy for existing tables to ensure clean migration.

**Process**:
1. Check if table exists
2. If exists, drop the table (with `DROP TABLE IF EXISTS`)
3. Create the table fresh with translated default values
4. Add constraints and indexes

## Configuration Enhancements

### Function Mappings Configuration

Users can now customize function mappings in their configuration files:

```json
{
  "migration": {
    "functionMappings": {
      "gen_random_uuid()": "RANDOM_UUID()",
      "CURRENT_TIMESTAMP": "CURRENT_TIMESTAMP",
      "custom_function()": "h2_equivalent()"
    }
  }
}
```

### Enhanced Logging

Added comprehensive logging to help debug migration issues:
- Function mapping initialization
- Default value translation
- Table and constraint existence checks
- Migration progress tracking

## Code Changes Made

### SchemaMigrator.java
- Added `functionMappings` field and initialization
- Added `translateDefaultValue()` method for default value translation
- Added existence checking methods for tables, indexes, and constraints
- Enhanced error handling and logging
- Added table drop-and-recreate logic

### DatabaseConnector.java
- Added `getConnection()` method to interface for accessing underlying JDBC connection

### MigrationConfig.java
- Added `functionMappings` field to MigrationSettings class
- Added getter and setter methods

### Configuration Files
- Updated `test-config.json` with function mappings
- Updated `default-config.json` with common function mappings

## Testing

Created comprehensive test suite in `SchemaMigratorTest.java` to verify:
- Function mapping functionality
- Type casting removal
- Array syntax handling
- Complex expression translation
- Edge case handling

## Usage

The fixes are automatically applied when migrating from PostgreSQL to H2. Users can:

1. **Use Default Mappings**: The tool automatically maps common PostgreSQL functions to H2 equivalents
2. **Customize Mappings**: Add custom function mappings in configuration files
3. **Monitor Progress**: Enhanced logging shows exactly what translations are being applied
4. **Handle Errors Gracefully**: The tool now checks for existing objects before creating new ones

## Migration Process

1. **Initialize**: Load function mappings based on source database type
2. **Extract Schema**: Get table metadata from source database
3. **Translate Defaults**: Convert all default values to H2-compatible syntax
4. **Create Tables**: Generate and execute CREATE TABLE statements
5. **Add Constraints**: Create indexes and foreign keys with existence checks
6. **Log Progress**: Provide detailed logging throughout the process

## Benefits

- **Reliability**: Migration now handles PostgreSQL-specific syntax automatically
- **Flexibility**: Users can customize function mappings for their specific needs
- **Debugging**: Comprehensive logging makes troubleshooting easier
- **Reusability**: Can run migration multiple times without constraint conflicts
- **Maintainability**: Clean separation of concerns with dedicated translation methods

# Migration Fixes: VARCHAR Precision Overflow

## Problem Description

The migration was failing with H2 database errors due to VARCHAR precision overflow:

```
org.h2.jdbc.JdbcSQLSyntaxErrorException: Precision ("2147483647") must be between "1" and "1000000000" inclusive
```

## Root Cause

PostgreSQL (and other databases) use `2147483647` (2^31 - 1) to represent unlimited VARCHAR/TEXT fields. When the migration code tried to create H2 tables with `VARCHAR(2147483647)`, H2 rejected it because its maximum VARCHAR precision is 1,000,000,000.

## Solution Implemented

### 1. Enhanced Data Type Mapping

Modified `SchemaMigrator.mapDataType()` to detect unlimited text fields and convert them to H2-compatible `CLOB` type:

```java
case "VARCHAR":
case "CHAR":
case "TEXT":
case "STRING":
    // Handle unlimited or very large text fields
    if (isUnlimitedOrVeryLargeSize(size)) {
        // For unlimited text or very large sizes, use CLOB in H2
        logger.debug("Converting large/unlimited text field (size: {}) to CLOB for type: {}", size, type);
        return "CLOB";
    }
    return "VARCHAR(" + size + ")";
```

### 2. Smart Size Detection

Added `isUnlimitedOrVeryLargeSize()` method to identify problematic column sizes:

```java
private boolean isUnlimitedOrVeryLargeSize(int size) {
    String dbType = sourceConnector.getDatabaseType();
    
    // Common values used by different databases to represent unlimited text
    return size <= 0 || 
           size == 2147483647 ||  // PostgreSQL unlimited VARCHAR/TEXT, MySQL LONGTEXT
           size == 65535 ||       // MySQL TEXT
           size == 16777215 ||    // MySQL MEDIUMTEXT
           size > config.getMigration().getMaxVarcharSizeThreshold(); // Configurable threshold
}
```

### 3. Configurable Threshold

Added `maxVarcharSizeThreshold` configuration option to allow customization:

```json
{
  "migration": {
    "maxVarcharSizeThreshold": 1000000,  // Default: 1MB
    // ... other settings
  }
}
```

### 4. Enhanced Logging and Validation

- Added detailed column metadata logging
- Added SQL validation before execution
- Added warnings for large columns
- Added database-specific size handling

## Configuration Options

### maxVarcharSizeThreshold

Controls when text fields should be converted to CLOB:

- **Default**: 1,000,000 (1MB)
- **Purpose**: Any column with size exceeding this threshold will be converted to CLOB
- **Usage**: Set lower for more aggressive CLOB conversion, higher to preserve more VARCHAR fields

### Example Configuration

```json
{
  "migration": {
    "maxVarcharSizeThreshold": 500000,  // Convert fields > 500KB to CLOB
    "dataTypeMappings": {
      "postgresql.text": "h2.clob",
      "mysql.longtext": "h2.clob"
    }
  }
}
```

## Database-Specific Handling

### PostgreSQL
- `VARCHAR(2147483647)` → `CLOB`
- `TEXT` → `CLOB` (if size > threshold)

### MySQL
- `TEXT` (size 65535) → `CLOB`
- `MEDIUMTEXT` (size 16777215) → `CLOB`
- `LONGTEXT` (size 2147483647) → `CLOB`

### Other Databases
- Any text field with size > threshold → `CLOB`

## Benefits

1. **Eliminates Migration Failures**: No more VARCHAR precision overflow errors
2. **Preserves Data Integrity**: Large text content is properly handled
3. **Configurable**: Users can adjust thresholds based on their needs
4. **Database Agnostic**: Works with PostgreSQL, MySQL, Oracle, SQL Server
5. **Better Performance**: CLOB is more efficient for large text in H2

## Migration Process

1. **Column Analysis**: Logs all column metadata with sizes
2. **Size Detection**: Identifies unlimited/very large text fields
3. **Type Mapping**: Converts problematic fields to CLOB
4. **SQL Validation**: Ensures generated SQL is H2-compatible
5. **Table Creation**: Creates tables with proper data types

## Testing

The fix has been tested with:
- PostgreSQL unlimited VARCHAR/TEXT fields
- MySQL TEXT/MEDIUMTEXT/LONGTEXT fields
- Various size thresholds
- Different database types

## Future Enhancements

1. **Additional Data Types**: Support for JSON, XML, and other complex types
2. **Performance Optimization**: Batch processing for large schemas
3. **Rollback Support**: Ability to revert failed migrations
4. **Migration Reports**: Detailed analysis of type conversions

## Troubleshooting

### Common Issues

1. **Still Getting Precision Errors**: Check if `maxVarcharSizeThreshold` is set too high
2. **Too Many CLOB Fields**: Increase the threshold value
3. **Performance Issues**: Consider using smaller thresholds for better H2 performance

### Debug Mode

Enable debug logging to see detailed column processing:

```json
{
  "output": {
    "logLevel": "DEBUG"
  }
}
```

This will show:
- Column metadata retrieval
- Size detection logic
- Type mapping decisions
- SQL generation process
- Validation results
