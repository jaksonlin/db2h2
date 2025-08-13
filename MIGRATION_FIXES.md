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
