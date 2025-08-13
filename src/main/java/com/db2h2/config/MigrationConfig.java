package com.db2h2.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Configuration class for migration settings
 */
public class MigrationConfig {
    
    @JsonProperty("source")
    private DatabaseConfig source;
    
    @JsonProperty("target")
    private DatabaseConfig target;
    
    @JsonProperty("migration")
    private MigrationSettings migration;
    
    @JsonProperty("output")
    private OutputSettings output;
    
    public MigrationConfig() {
        this.migration = new MigrationSettings();
        this.output = new OutputSettings();
    }
    
    // Getters and Setters
    public DatabaseConfig getSource() {
        return source;
    }
    
    public void setSource(DatabaseConfig source) {
        this.source = source;
    }
    
    public DatabaseConfig getTarget() {
        return target;
    }
    
    public void setTarget(DatabaseConfig target) {
        this.target = target;
    }
    
    public MigrationSettings getMigration() {
        return migration;
    }
    
    public void setMigration(MigrationSettings migration) {
        this.migration = migration;
    }
    
    public OutputSettings getOutput() {
        return output;
    }
    
    public void setOutput(OutputSettings output) {
        this.output = output;
    }
    
    /**
     * Migration-specific settings
     */
    public static class MigrationSettings {
        @JsonProperty("tables")
        private List<String> tables;
        
        @JsonProperty("excludeTables")
        private List<String> excludeTables;
        
        @JsonProperty("batchSize")
        private Integer batchSize = 1000;
        
        @JsonProperty("preserveData")
        private Boolean preserveData = true;
        
        @JsonProperty("dataTypeMappings")
        private Map<String, String> dataTypeMappings;
        
        @JsonProperty("functionMappings")
        private Map<String, String> functionMappings;
        
        @JsonProperty("constraints")
        private ConstraintSettings constraints;
        
        @JsonProperty("data")
        private DataSettings data;
        
        public MigrationSettings() {
            this.constraints = new ConstraintSettings();
            this.data = new DataSettings();
        }
        
        // Getters and Setters
        public List<String> getTables() {
            return tables;
        }
        
        public void setTables(List<String> tables) {
            this.tables = tables;
        }
        
        public List<String> getExcludeTables() {
            return excludeTables;
        }
        
        public void setExcludeTables(List<String> excludeTables) {
            this.excludeTables = excludeTables;
        }
        
        public Integer getBatchSize() {
            return batchSize;
        }
        
        public void setBatchSize(Integer batchSize) {
            this.batchSize = batchSize;
        }
        
        public Boolean getPreserveData() {
            return preserveData;
        }
        
        public void setPreserveData(Boolean preserveData) {
            this.preserveData = preserveData;
        }
        
        public Map<String, String> getDataTypeMappings() {
            return dataTypeMappings;
        }
        
        public void setDataTypeMappings(Map<String, String> dataTypeMappings) {
            this.dataTypeMappings = dataTypeMappings;
        }
        
        public Map<String, String> getFunctionMappings() {
            return functionMappings;
        }
        
        public void setFunctionMappings(Map<String, String> functionMappings) {
            this.functionMappings = functionMappings;
        }
        
        public ConstraintSettings getConstraints() {
            return constraints;
        }
        
        public void setConstraints(ConstraintSettings constraints) {
            this.constraints = constraints;
        }
        
        public DataSettings getData() {
            return data;
        }
        
        public void setData(DataSettings data) {
            this.data = data;
        }
    }
    
    /**
     * Constraint migration settings
     */
    public static class ConstraintSettings {
        @JsonProperty("preserveForeignKeys")
        private Boolean preserveForeignKeys = true;
        
        @JsonProperty("preserveIndexes")
        private Boolean preserveIndexes = true;
        
        @JsonProperty("preserveTriggers")
        private Boolean preserveTriggers = false;
        
        @JsonProperty("preserveSequences")
        private Boolean preserveSequences = true;
        
        public Boolean getPreserveForeignKeys() {
            return preserveForeignKeys;
        }
        
        public void setPreserveForeignKeys(Boolean preserveForeignKeys) {
            this.preserveForeignKeys = preserveForeignKeys;
        }
        
        public Boolean getPreserveIndexes() {
            return preserveIndexes;
        }
        
        public void setPreserveIndexes(Boolean preserveIndexes) {
            this.preserveIndexes = preserveIndexes;
        }
        
        public Boolean getPreserveTriggers() {
            return preserveTriggers;
        }
        
        public void setPreserveTriggers(Boolean preserveTriggers) {
            this.preserveTriggers = preserveTriggers;
        }
        
        public Boolean getPreserveSequences() {
            return preserveSequences;
        }
        
        public void setPreserveSequences(Boolean preserveSequences) {
            this.preserveSequences = preserveSequences;
        }
    }
    
    /**
     * Data migration settings
     */
    public static class DataSettings {
        @JsonProperty("maxRows")
        private Integer maxRows;
        
        @JsonProperty("sampleData")
        private Boolean sampleData = false;
        
        @JsonProperty("samplePercentage")
        private Integer samplePercentage = 10;
        
        @JsonProperty("validateData")
        private Boolean validateData = true;
        
        @JsonProperty("anonymizeData")
        private Boolean anonymizeData = false;
        
        @JsonProperty("anonymizationRules")
        private Map<String, String> anonymizationRules;
        
        public Integer getMaxRows() {
            return maxRows;
        }
        
        public void setMaxRows(Integer maxRows) {
            this.maxRows = maxRows;
        }
        
        public Boolean getSampleData() {
            return sampleData;
        }
        
        public void setSampleData(Boolean sampleData) {
            this.sampleData = sampleData;
        }
        
        public Integer getSamplePercentage() {
            return samplePercentage;
        }
        
        public void setSamplePercentage(Integer samplePercentage) {
            this.samplePercentage = samplePercentage;
        }
        
        public Boolean getValidateData() {
            return validateData;
        }
        
        public void setValidateData(Boolean validateData) {
            this.validateData = validateData;
        }
        
        public Boolean getAnonymizeData() {
            return anonymizeData;
        }
        
        public void setAnonymizeData(Boolean anonymizeData) {
            this.anonymizeData = anonymizeData;
        }
        
        public Map<String, String> getAnonymizationRules() {
            return anonymizationRules;
        }
        
        public void setAnonymizationRules(Map<String, String> anonymizationRules) {
            this.anonymizationRules = anonymizationRules;
        }
    }
    
    /**
     * Output settings
     */
    public static class OutputSettings {
        @JsonProperty("generateReport")
        private Boolean generateReport = true;
        
        @JsonProperty("reportFile")
        private String reportFile = "./migration-report.html";
        
        @JsonProperty("logLevel")
        private String logLevel = "INFO";
        
        @JsonProperty("outputDir")
        private String outputDir = "./output";
        
        @JsonProperty("exitOnError")
        private Boolean exitOnError = false;
        
        public Boolean getGenerateReport() {
            return generateReport;
        }
        
        public void setGenerateReport(Boolean generateReport) {
            this.generateReport = generateReport;
        }
        
        public String getReportFile() {
            return reportFile;
        }
        
        public void setReportFile(String reportFile) {
            this.reportFile = reportFile;
        }
        
        public String getLogLevel() {
            return logLevel;
        }
        
        public void setLogLevel(String logLevel) {
            this.logLevel = logLevel;
        }
        
        public String getOutputDir() {
            return outputDir;
        }
        
        public void setOutputDir(String outputDir) {
            this.outputDir = outputDir;
        }
        
        public Boolean getExitOnError() {
            return exitOnError;
        }
        
        public void setExitOnError(Boolean exitOnError) {
            this.exitOnError = exitOnError;
        }
    }
    
    /**
     * Validates the configuration
     */
    public void validate() {
        if (source == null) {
            throw new IllegalArgumentException("Source database configuration is required");
        }
        
        if (target == null) {
            throw new IllegalArgumentException("Target database configuration is required");
        }
        
        if (source.getType() == null || source.getType().trim().isEmpty()) {
            throw new IllegalArgumentException("Source database type is required");
        }
        
        if (target.getType() == null || !"h2".equalsIgnoreCase(target.getType())) {
            throw new IllegalArgumentException("Target database must be H2");
        }
        
        // Validate source database connection parameters
        if (source.getHost() == null || source.getHost().trim().isEmpty()) {
            throw new IllegalArgumentException("Source database host is required");
        }
        
        if (source.getDatabase() == null || source.getDatabase().trim().isEmpty()) {
            throw new IllegalArgumentException("Source database name is required");
        }
        
        if (source.getUsername() == null || source.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Source database username is required");
        }
    }
    
    @Override
    public String toString() {
        return "MigrationConfig{" +
                "source=" + source +
                ", target=" + target +
                ", migration=" + migration +
                ", output=" + output +
                '}';
    }
} 