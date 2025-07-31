package com.db2h2.cli;

import com.db2h2.config.MigrationConfig;
import com.db2h2.core.MigrationEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Command-line interface for the DB2H2 migration tool
 */
public class MigrationCLI {
    
    private static final Logger logger = LoggerFactory.getLogger(MigrationCLI.class);
    
    public static void main(String[] args) {
        MigrationCLI cli = new MigrationCLI();
        cli.run(args);
    }
    
    public void run(String[] args) {
        try {
            // Parse command line arguments
            CommandLine cmd = parseArguments(args);
            
            if (cmd.hasOption("help")) {
                printHelp();
                return;
            }
            
            // Load configuration
            MigrationConfig config = loadConfiguration(cmd);
            
            // Validate configuration
            config.validate();
            
            // Execute migration
            MigrationEngine engine = new MigrationEngine(config);
            MigrationEngine.MigrationResult result = engine.migrate();
            
            // Handle result
            if (result.isSuccess()) {
                logger.info("Migration completed successfully!");
                logger.info("Target database: {}", result.getTargetDatabase());
                logger.info("Tables migrated: {}", result.getTablesMigrated());
                logger.info("Duration: {} ms", result.getDuration());
                System.exit(0);
            } else {
                logger.error("Migration failed: {}", result.getMessage());
                if (result.getError() != null) {
                    result.getError().printStackTrace();
                }
                System.exit(1);
            }
            
        } catch (Exception e) {
            logger.error("Error running migration: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
    
    /**
     * Parses command line arguments
     */
    private CommandLine parseArguments(String[] args) throws ParseException {
        Options options = createOptions();
        CommandLineParser parser = new DefaultParser();
        return parser.parse(options, args);
    }
    
    /**
     * Creates command line options
     */
    private Options createOptions() {
        Options options = new Options();
        
        // Configuration file option
        options.addOption(Option.builder("c")
                .longOpt("config")
                .hasArg()
                .desc("Configuration file path")
                .build());
        
        // Source database options
        options.addOption(Option.builder("s")
                .longOpt("source-type")
                .hasArg()
                .desc("Source database type (postgresql, mysql, oracle, sqlserver)")
                .build());
        
        options.addOption(Option.builder()
                .longOpt("source-host")
                .hasArg()
                .desc("Source database host")
                .build());
        
        options.addOption(Option.builder()
                .longOpt("source-port")
                .hasArg()
                .desc("Source database port")
                .build());
        
        options.addOption(Option.builder()
                .longOpt("source-database")
                .hasArg()
                .desc("Source database name")
                .build());
        
        options.addOption(Option.builder()
                .longOpt("source-username")
                .hasArg()
                .desc("Source database username")
                .build());
        
        options.addOption(Option.builder()
                .longOpt("source-password")
                .hasArg()
                .desc("Source database password")
                .build());
        
        // Target database options
        options.addOption(Option.builder("t")
                .longOpt("target-file")
                .hasArg()
                .desc("Target H2 database file path")
                .build());
        
        // Migration options
        options.addOption(Option.builder()
                .longOpt("tables")
                .hasArg()
                .desc("Comma-separated list of tables to migrate (use '*' for all)")
                .build());
        
        options.addOption(Option.builder()
                .longOpt("exclude-tables")
                .hasArg()
                .desc("Comma-separated list of tables to exclude")
                .build());
        
        options.addOption(Option.builder()
                .longOpt("batch-size")
                .hasArg()
                .desc("Batch size for data migration (default: 1000)")
                .build());
        
        options.addOption(Option.builder()
                .longOpt("max-rows")
                .hasArg()
                .desc("Maximum number of rows to migrate per table")
                .build());
        
        // Output options
        options.addOption(Option.builder()
                .longOpt("output-dir")
                .hasArg()
                .desc("Output directory for reports")
                .build());
        
        options.addOption(Option.builder()
                .longOpt("exit-on-error")
                .desc("Exit immediately on error")
                .build());
        
        // Help option
        options.addOption(Option.builder("h")
                .longOpt("help")
                .desc("Show help message")
                .build());
        
        return options;
    }
    
    /**
     * Loads configuration from file or command line arguments
     */
    private MigrationConfig loadConfiguration(CommandLine cmd) throws IOException {
        MigrationConfig config = new MigrationConfig();
        
        // Try to load from config file first
        if (cmd.hasOption("config")) {
            config = loadConfigFromFile(cmd.getOptionValue("config"));
        }
        
        // Override with command line arguments
        overrideConfigFromCommandLine(config, cmd);
        
        return config;
    }
    
    /**
     * Loads configuration from JSON file
     */
    private MigrationConfig loadConfigFromFile(String configPath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        File configFile = new File(configPath);
        
        if (!configFile.exists()) {
            throw new IOException("Configuration file not found: " + configPath);
        }
        
        logger.info("Loading configuration from: {}", configPath);
        return mapper.readValue(configFile, MigrationConfig.class);
    }
    
    /**
     * Overrides configuration with command line arguments
     */
    private void overrideConfigFromCommandLine(MigrationConfig config, CommandLine cmd) {
        // Source database overrides
        if (cmd.hasOption("source-type")) {
            config.getSource().setType(cmd.getOptionValue("source-type"));
        }
        
        if (cmd.hasOption("source-host")) {
            config.getSource().setHost(cmd.getOptionValue("source-host"));
        }
        
        if (cmd.hasOption("source-port")) {
            config.getSource().setPort(Integer.parseInt(cmd.getOptionValue("source-port")));
        }
        
        if (cmd.hasOption("source-database")) {
            config.getSource().setDatabase(cmd.getOptionValue("source-database"));
        }
        
        if (cmd.hasOption("source-username")) {
            config.getSource().setUsername(cmd.getOptionValue("source-username"));
        }
        
        if (cmd.hasOption("source-password")) {
            config.getSource().setPassword(cmd.getOptionValue("source-password"));
        }
        
        // Target database overrides
        if (cmd.hasOption("target-file")) {
            config.getTarget().setFile(cmd.getOptionValue("target-file"));
        }
        
        // Migration overrides
        if (cmd.hasOption("tables")) {
            String[] tables = cmd.getOptionValue("tables").split(",");
            config.getMigration().setTables(java.util.Arrays.asList(tables));
        }
        
        if (cmd.hasOption("exclude-tables")) {
            String[] excludeTables = cmd.getOptionValue("exclude-tables").split(",");
            config.getMigration().setExcludeTables(java.util.Arrays.asList(excludeTables));
        }
        
        if (cmd.hasOption("batch-size")) {
            config.getMigration().setBatchSize(Integer.parseInt(cmd.getOptionValue("batch-size")));
        }
        
        if (cmd.hasOption("max-rows")) {
            config.getMigration().getData().setMaxRows(Integer.parseInt(cmd.getOptionValue("max-rows")));
        }
        
        // Output overrides
        if (cmd.hasOption("output-dir")) {
            config.getOutput().setOutputDir(cmd.getOptionValue("output-dir"));
        }
        
        if (cmd.hasOption("exit-on-error")) {
            config.getOutput().setExitOnError(true);
        }
    }
    
    /**
     * Prints help message
     */
    private void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("db2h2", "RDBMS to H2 Database Migration Tool", createOptions(), 
                           "Example: java -jar db2h2.jar --config config.json", true);
    }
} 