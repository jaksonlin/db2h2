# Auto-Increment Sequence Migration Guide

## Overview

When migrating databases with auto-increment/sequence columns, you may need to preserve the exact data values to maintain referential integrity and data consistency. The DB2H2 tool provides a feature to temporarily disable auto-increment during migration and re-enable it afterward.

## Why Disable Auto-Increment During Migration?

### Problem
- Auto-increment columns automatically generate new values when inserting data
- During migration, this can cause data inconsistencies
- Referential relationships may break if primary key values change
- Audit trails and business logic may depend on specific ID values

### Solution
- Temporarily disable auto-increment during schema creation
- Migrate data with original values preserved
- Re-enable auto-increment after data migration
- Maintain exact data integrity throughout the process

## Configuration

### Enable the Feature

Add the following configuration to your migration config:

```json
{
  "migration": {
    "constraints": {
      "disableAutoIncrementDuringMigration": true
    }
  }
}
```

### Complete Example Configuration

```json
{
  "source": {
    "type": "mysql",
    "host": "localhost",
    "port": 3306,
    "database": "source_db",
    "username": "user",
    "password": "password"
  },
  "target": {
    "type": "h2",
    "file": "./target-db.h2.db",
    "mode": "file"
  },
  "migration": {
    "tables": ["users", "orders", "products"],
    "preserveData": true,
    "constraints": {
      "preserveForeignKeys": true,
      "preserveIndexes": true,
      "preserveSequences": true,
      "disableAutoIncrementDuringMigration": true
    }
  }
}
```

## How It Works

### 1. Schema Migration Phase
- Tables are created **without** `AUTO_INCREMENT` constraints
- All other constraints (foreign keys, indexes) are preserved
- Column types and sizes remain the same

### 2. Data Migration Phase
- Data is inserted with original values preserved
- No automatic value generation occurs
- Referential integrity is maintained

### 3. Post-Migration Phase
- Auto-increment is re-enabled on all affected columns
- Future inserts will generate new sequential values
- Sequences are properly initialized

## Migration Flow

```
Schema Migration → Disable Auto-Increment → Data Migration → Re-enable Auto-Increment → Finalize
```

### Detailed Steps

1. **Schema Creation**: Tables created without auto-increment
2. **Auto-Increment Disabling**: Preparation for data migration
3. **Data Transfer**: Data copied with original values
4. **Auto-Increment Re-enabling**: Restore auto-increment functionality
5. **Sequence Updates**: Initialize sequences for future use

## Use Cases

### Production Database Cloning
- Create exact copies for testing environments
- Maintain data relationships and business logic
- Preserve audit trails and historical data

### Data Migration Projects
- Move data between different database systems
- Ensure referential integrity preservation
- Maintain data consistency across environments

### Testing and Development
- Create realistic test datasets
- Maintain production-like data relationships
- Support integration testing scenarios

## Example Scenarios

### Scenario 1: E-commerce Database

**Source Table Structure:**
```sql
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE,
    email VARCHAR(100)
);

CREATE TABLE orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    total DECIMAL(10,2),
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

**Migration Process:**
1. Tables created without auto-increment
2. Data migrated with original ID values
3. Foreign key relationships preserved
4. Auto-increment re-enabled for future orders

### Scenario 2: Multi-tenant Application

**Source Table Structure:**
```sql
CREATE TABLE tenants (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100)
);

CREATE TABLE tenant_data (
    id INT AUTO_INCREMENT PRIMARY KEY,
    tenant_id INT,
    data_value TEXT,
    FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);
```

**Benefits:**
- Tenant relationships maintained
- Data isolation preserved
- Future tenant creation works correctly

## Configuration Options

### Related Settings

| Setting | Description | Default |
|---------|-------------|---------|
| `disableAutoIncrementDuringMigration` | Enable/disable the feature | `false` |
| `preserveSequences` | Handle sequence updates | `true` |
| `preserveForeignKeys` | Maintain referential integrity | `true` |
| `preserveIndexes` | Keep table indexes | `true` |

### Performance Considerations

- **Schema Recreation**: May impact large tables
- **Data Migration**: No additional overhead
- **Post-Processing**: Minimal impact on completion time

## Troubleshooting

### Common Issues

#### Issue: Auto-increment not re-enabled
**Symptoms:**
- Tables created without auto-increment
- Future inserts fail or generate errors

**Solution:**
- Check configuration: `disableAutoIncrementDuringMigration: true`
- Verify migration completed successfully
- Check logs for re-enabling errors

#### Issue: Foreign key constraint violations
**Symptoms:**
- Migration fails with constraint errors
- Data relationships broken

**Solution:**
- Ensure `preserveForeignKeys: true`
- Check table migration order
- Verify source data integrity

### Log Messages

Look for these log messages during migration:

```
INFO  - Disabling auto-increment on tables...
DEBUG - Table users has auto-increment columns, will be recreated without auto-increment
INFO  - Auto-increment disabling preparation completed
...
INFO  - Re-enabling auto-increment on tables...
DEBUG - Re-enabled auto-increment on column id in table users
INFO  - Auto-increment re-enabling completed
```

## Best Practices

### 1. Test First
- Always test with a subset of data
- Verify referential integrity after migration
- Check auto-increment functionality

### 2. Backup Strategy
- Backup source database before migration
- Test migration on non-production data
- Have rollback plan ready

### 3. Monitoring
- Monitor migration progress
- Check logs for any errors
- Verify final database state

### 4. Validation
- Compare record counts
- Verify data relationships
- Test application functionality

## Limitations

### Current Limitations
- H2 database specific implementation
- Requires table recreation for some operations
- Limited to supported database types

### Future Enhancements
- Support for more database systems
- Improved table alteration methods
- Better sequence handling

## Support

For issues or questions:
- Check the main README.md
- Review migration logs
- Verify configuration settings
- Test with minimal dataset first
