# RDBMS to H2 Database Migration Tool - Design Document

## Project Overview
A Java-based tool that connects to any RDBMS (PostgreSQL, MySQL, Oracle, SQL Server, etc.) and dumps its database schema and data into H2 database format for CI/CD test automation purposes.

## Core Architecture

### 1. Modular Database Connector System
- **Abstract Database Connector**: Base interface for all database connections
- **Database-Specific Implementations**: 
  - PostgreSQL Connector
  - MySQL Connector
  - Oracle Connector
  - SQL Server Connector
  - SQLite Connector
  - H2 Connector (target)

### 2. Schema Migration Engine
- **Schema Analyzer**: Extracts table structures, constraints, indexes
- **Data Type Mapper**: Maps RDBMS-specific data types to H2 equivalents
- **Constraint Translator**: Converts foreign keys, primary keys, unique constraints
- **Index Generator**: Recreates indexes in H2 format

### 3. Data Migration Pipeline
- **Data Extractor**: Reads data from source database
- **Data Transformer**: Converts data types and formats
- **Data Loader**: Inserts data into H2 database
- **Batch Processor**: Handles large datasets efficiently

### 4. Configuration Management
- **Source Database Config**: Connection parameters for source RDBMS
- **Target H2 Config**: H2 database settings
- **Migration Rules**: Custom mapping rules and filters
- **Table Selection**: Include/exclude specific tables

## Key Features

### 1. Universal Database Support
- JDBC-based connections for maximum compatibility
- Automatic database type detection
- Custom connection pooling

### 2. Schema Preservation
- Maintains table relationships
- Preserves data types where possible
- Handles complex constraints
- Supports custom data type mappings

### 3. Data Integrity
- Transaction-based migration
- Data validation and verification
- Rollback capabilities
- Progress tracking and logging

### 4. CI/CD Integration
- Command-line interface
- Configuration file support
- Exit codes for automation
- Docker containerization

## Technical Stack

### Core Technologies
- **Java 11+**: Main application language
- **Maven/Gradle**: Build and dependency management
- **JDBC**: Database connectivity
- **H2 Database**: Target database engine

### Database Drivers
- PostgreSQL: `postgresql-jdbc`
- MySQL: `mysql-connector-java`
- Oracle: `ojdbc8`
- SQL Server: `mssql-jdbc`
- H2: `h2` (embedded)

### Additional Libraries
- **Apache Commons**: Configuration and utilities
- **SLF4J + Logback**: Logging framework
- **Jackson**: JSON configuration handling
- **JUnit 5**: Unit testing

## Project Structure

```
db2h2/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── db2h2/
│   │   │           ├── core/
│   │   │           │   ├── DatabaseConnector.java
│   │   │           │   ├── SchemaMigrator.java
│   │   │           │   ├── DataMigrator.java
│   │   │           │   └── MigrationEngine.java
│   │   │           ├── connectors/
│   │   │           │   ├── AbstractDatabaseConnector.java
│   │   │           │   ├── PostgresConnector.java
│   │   │           │   ├── MySqlConnector.java
│   │   │           │   ├── OracleConnector.java
│   │   │           │   ├── SqlServerConnector.java
│   │   │           │   └── H2Connector.java
│   │   │           ├── mappers/
│   │   │           │   ├── DataTypeMapper.java
│   │   │           │   ├── ConstraintMapper.java
│   │   │           │   └── IndexMapper.java
│   │   │           ├── config/
│   │   │           │   ├── DatabaseConfig.java
│   │   │           │   ├── MigrationConfig.java
│   │   │           │   └── ConfigLoader.java
│   │   │           ├── utils/
│   │   │           │   ├── SqlGenerator.java
│   │   │           │   ├── DataValidator.java
│   │   │           │   └── ProgressTracker.java
│   │   │           └── cli/
│   │   │               └── MigrationCLI.java
│   │   └── resources/
│   │       ├── config/
│   │       │   ├── default-config.json
│   │       │   └── data-type-mappings.json
│   │       └── templates/
│   │           └── h2-schema.sql
│   └── test/
│       └── java/
│           └── com/
│               └── db2h2/
│                   ├── unit/
│                   └── integration/
├── docs/
│   ├── README.md
│   ├── USAGE.md
│   └── EXAMPLES.md
├── scripts/
│   ├── run.sh
│   └── run.bat
├── docker/
│   ├── Dockerfile
│   └── docker-compose.yml
├── pom.xml
└── .gitignore
```

## Migration Process Flow

### 1. Initialization Phase
1. Load configuration from file or command line
2. Validate source database connectivity
3. Initialize target H2 database
4. Create migration session

### 2. Schema Analysis Phase
1. Extract table metadata from source database
2. Analyze relationships and constraints
3. Map data types to H2 equivalents
4. Generate H2 schema DDL

### 3. Schema Migration Phase
1. Create tables in H2 database
2. Apply constraints and indexes
3. Validate schema consistency
4. Generate migration report

### 4. Data Migration Phase
1. Extract data from source tables
2. Transform data types and formats
3. Load data into H2 tables
4. Verify data integrity

### 5. Finalization Phase
1. Update sequences and auto-increment values
2. Generate final migration report
3. Clean up temporary resources
4. Provide H2 database file

## Configuration Examples

### Basic Configuration
```json
{
  "source": {
    "type": "postgresql",
    "host": "localhost",
    "port": 5432,
    "database": "source_db",
    "username": "user",
    "password": "password"
  },
  "target": {
    "type": "h2",
    "file": "./target/test-db.h2.db",
    "mode": "file"
  },
  "migration": {
    "tables": ["users", "orders", "products"],
    "excludeTables": ["temp_*", "log_*"],
    "batchSize": 1000,
    "preserveData": true
  }
}
```

### Advanced Configuration
```json
{
  "source": {
    "type": "mysql",
    "host": "prod-db.company.com",
    "port": 3306,
    "database": "production",
    "username": "readonly_user",
    "password": "secure_password",
    "ssl": true,
    "connectionPool": {
      "maxConnections": 10,
      "timeout": 30000
    }
  },
  "target": {
    "type": "h2",
    "file": "./ci/test-database.h2.db",
    "mode": "file",
    "compression": true
  },
  "migration": {
    "tables": ["*"],
    "excludeTables": ["audit_log", "temp_*"],
    "dataTypeMappings": {
      "postgresql.uuid": "h2.uuid",
      "mysql.json": "h2.varchar(4000)"
    },
    "constraints": {
      "preserveForeignKeys": true,
      "preserveIndexes": true,
      "preserveTriggers": false
    },
    "data": {
      "batchSize": 5000,
      "maxRows": 100000,
      "sampleData": false
    }
  },
  "output": {
    "generateReport": true,
    "reportFile": "./migration-report.html",
    "logLevel": "INFO"
  }
}
```

## Usage Examples

### Command Line Usage
```bash
# Basic usage
java -jar db2h2.jar --config config.json

# Direct parameters
java -jar db2h2.jar \
  --source-type postgresql \
  --source-host localhost \
  --source-database testdb \
  --target-file ./h2-db.h2.db

# CI/CD pipeline
java -jar db2h2.jar \
  --config ci-config.json \
  --output-dir ./test-data \
  --exit-on-error
```

### Programmatic Usage
```java
MigrationConfig config = new MigrationConfig();
config.setSourceDatabase("postgresql://localhost:5432/source_db");
config.setTargetDatabase("h2://./target/test-db.h2.db");

MigrationEngine engine = new MigrationEngine(config);
MigrationResult result = engine.migrate();

if (result.isSuccess()) {
    System.out.println("Migration completed successfully");
    System.out.println("H2 database: " + result.getTargetDatabase());
}
```

## Benefits for CI/CD

### 1. Fast Test Database Setup
- Pre-populated test data
- Consistent schema across environments
- No external database dependencies

### 2. Isolated Testing
- Each test run gets fresh database
- No data pollution between tests
- Parallel test execution support

### 3. Version Control Integration
- H2 database files can be versioned
- Schema changes tracked in git
- Automated database updates

### 4. Performance Optimization
- In-memory H2 databases for speed
- Optimized indexes for test queries
- Minimal data for faster startup

## Future Enhancements

### 1. Advanced Features
- Incremental migrations
- Data anonymization for sensitive data
- Custom data generators
- Multi-database support

### 2. Integration Features
- Jenkins plugin
- GitHub Actions integration
- Docker image with pre-built databases
- REST API for remote execution

### 3. Monitoring and Analytics
- Migration performance metrics
- Data quality reports
- Schema drift detection
- Automated testing recommendations 