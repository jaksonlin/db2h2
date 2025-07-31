# DB2H2 - RDBMS to H2 Database Migration Tool

A Java-based tool that connects to any RDBMS (PostgreSQL, MySQL, Oracle, SQL Server, etc.) and dumps its database schema and data into H2 database format for CI/CD test automation purposes.

## Features

- **Universal Database Support**: Connect to PostgreSQL, MySQL, Oracle, SQL Server, and more
- **Schema Migration**: Preserve table structures, constraints, indexes, and relationships
- **Data Migration**: Transfer data with configurable batch sizes and sampling options
- **Data Anonymization**: Protect sensitive data during migration
- **CI/CD Integration**: Command-line interface with exit codes for automation
- **Progress Tracking**: Real-time progress monitoring and detailed logging
- **Configuration Management**: JSON-based configuration with command-line overrides

## Quick Start

### Prerequisites

- Java 11 or higher
- Maven 3.6 or higher
- Access to source database

### Installation

1. Clone the repository:
```bash
git clone https://github.com/your-username/db2h2.git
cd db2h2
```

2. Build the project:
```bash
mvn clean package
```

3. Run the migration:
```bash
java -jar target/db2h2-migration-1.0.0.jar --config config.json
```

## Configuration

### Basic Configuration File

Create a `config.json` file:

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
    "batchSize": 1000,
    "preserveData": true
  }
}
```

### Command Line Usage

```bash
# Basic usage with config file
java -jar db2h2.jar --config config.json

# Direct parameters
java -jar db2h2.jar \
  --source-type postgresql \
  --source-host localhost \
  --source-database testdb \
  --source-username user \
  --source-password password \
  --target-file ./h2-db.h2.db

# CI/CD pipeline with specific tables
java -jar db2h2.jar \
  --config ci-config.json \
  --tables "users,orders,products" \
  --batch-size 5000 \
  --exit-on-error
```

## Supported Databases

### Source Databases

- **PostgreSQL**: Full support with UUID and JSON handling
- **MySQL**: Complete schema and data migration
- **Oracle**: Enterprise database migration support
- **SQL Server**: Microsoft SQL Server compatibility
- **H2**: Can also migrate from H2 to H2

### Target Database

- **H2**: File-based or in-memory database
- **Compression**: Optional database compression
- **Mode**: File or memory mode support

## Advanced Configuration

### Data Anonymization

```json
{
  "migration": {
    "data": {
      "anonymizeData": true,
      "anonymizationRules": {
        "email": "hash",
        "phone": "random",
        "password": "null",
        "ssn": "mask"
      }
    }
  }
}
```

### Custom Data Type Mappings

```json
{
  "migration": {
    "dataTypeMappings": {
      "postgresql.uuid": "h2.uuid",
      "mysql.json": "h2.varchar(4000)",
      "oracle.clob": "h2.clob"
    }
  }
}
```

### Sampling and Limits

```json
{
  "migration": {
    "data": {
      "sampleData": true,
      "samplePercentage": 10,
      "maxRows": 10000
    }
  }
}
```

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Database Migration
on: [push, pull_request]

jobs:
  migrate-db:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      
      - name: Set up Java
        uses: actions/setup-java@v2
        with:
          java-version: '11'
          
      - name: Build and Run Migration
        run: |
          mvn clean package
          java -jar target/db2h2-migration-1.0.0.jar \
            --config ci-config.json \
            --exit-on-error
```

### Jenkins Pipeline Example

```groovy
pipeline {
    agent any
    
    stages {
        stage('Migrate Database') {
            steps {
                sh '''
                    mvn clean package
                    java -jar target/db2h2-migration-1.0.0.jar \
                      --config ci-config.json \
                      --exit-on-error
                '''
            }
        }
    }
}
```

## Programmatic Usage

```java
import com.db2h2.config.MigrationConfig;
import com.db2h2.core.MigrationEngine;

// Create configuration
MigrationConfig config = new MigrationConfig();
config.getSource().setType("postgresql");
config.getSource().setHost("localhost");
config.getSource().setDatabase("source_db");
config.getSource().setUsername("user");
config.getSource().setPassword("password");

config.getTarget().setType("h2");
config.getTarget().setFile("./target/test-db.h2.db");

// Execute migration
MigrationEngine engine = new MigrationEngine(config);
MigrationEngine.MigrationResult result = engine.migrate();

if (result.isSuccess()) {
    System.out.println("Migration completed successfully");
    System.out.println("H2 database: " + result.getTargetDatabase());
    System.out.println("Tables migrated: " + result.getTablesMigrated());
    System.out.println("Duration: " + result.getDuration() + " ms");
} else {
    System.err.println("Migration failed: " + result.getMessage());
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

## Project Structure

```
db2h2/
├── src/main/java/com/db2h2/
│   ├── core/                 # Core migration engine
│   ├── connectors/           # Database connectors
│   ├── config/              # Configuration classes
│   ├── utils/               # Utility classes
│   └── cli/                 # Command-line interface
├── src/main/resources/      # Configuration templates
├── src/test/               # Unit and integration tests
├── docs/                   # Documentation
├── scripts/                # Build and run scripts
└── docker/                 # Docker configuration
```

## Development

### Building from Source

```bash
# Clone repository
git clone https://github.com/jaksonlin/db2h2.git
cd db2h2

# Build project
mvn clean package

# Run tests
mvn test

# Run integration tests
mvn verify
```

### Adding New Database Support

1. Create a new connector class extending `AbstractDatabaseConnector`
2. Implement database-specific logic in the connector
3. Add the connector to `DatabaseConnectorFactory`
4. Add appropriate JDBC driver dependency to `pom.xml`
5. Write tests for the new connector

### Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## Troubleshooting

### Common Issues

1. **Connection Failed**: Check database credentials and network connectivity
2. **Schema Migration Errors**: Verify database permissions and table access
3. **Data Type Mapping Issues**: Review custom data type mappings
4. **Memory Issues**: Reduce batch size or enable data sampling

### Logging

The tool uses SLF4J with Logback. Configure logging in `logback.xml`:

```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="STDOUT" />
    </root>
</configuration>
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

- **Issues**: [GitHub Issues](https://github.com/your-username/db2h2/issues)
- **Documentation**: [Wiki](https://github.com/your-username/db2h2/wiki)
- **Discussions**: [GitHub Discussions](https://github.com/your-username/db2h2/discussions)

## Roadmap

- [ ] Incremental migrations
- [ ] Data quality reports
- [ ] Schema drift detection
- [ ] REST API interface
- [ ] Docker image with pre-built databases
- [ ] Jenkins plugin
- [ ] GitHub Actions integration 