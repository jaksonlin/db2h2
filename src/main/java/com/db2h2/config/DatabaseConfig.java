package com.db2h2.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration class for database connections
 */
public class DatabaseConfig {
    
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("host")
    private String host;
    
    @JsonProperty("port")
    private Integer port;
    
    @JsonProperty("database")
    private String database;
    
    @JsonProperty("username")
    private String username;
    
    @JsonProperty("password")
    private String password;
    
    @JsonProperty("file")
    private String file;
    
    @JsonProperty("mode")
    private String mode = "file";
    
    @JsonProperty("ssl")
    private Boolean ssl = false;
    
    @JsonProperty("connectionPool")
    private ConnectionPoolConfig connectionPool;
    
    @JsonProperty("compression")
    private Boolean compression = false;
    
    public DatabaseConfig() {}
    
    public DatabaseConfig(String type, String host, Integer port, String database, 
                         String username, String password) {
        this.type = type;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
    }
    
    // Getters and Setters
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getHost() {
        return host;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    public Integer getPort() {
        return port;
    }
    
    public void setPort(Integer port) {
        this.port = port;
    }
    
    public String getDatabase() {
        return database;
    }
    
    public void setDatabase(String database) {
        this.database = database;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getFile() {
        return file;
    }
    
    public void setFile(String file) {
        this.file = file;
    }
    
    public String getMode() {
        return mode;
    }
    
    public void setMode(String mode) {
        this.mode = mode;
    }
    
    public Boolean getSsl() {
        return ssl;
    }
    
    public void setSsl(Boolean ssl) {
        this.ssl = ssl;
    }
    
    public ConnectionPoolConfig getConnectionPool() {
        return connectionPool;
    }
    
    public void setConnectionPool(ConnectionPoolConfig connectionPool) {
        this.connectionPool = connectionPool;
    }
    
    public Boolean getCompression() {
        return compression;
    }
    
    public void setCompression(Boolean compression) {
        this.compression = compression;
    }
    
    /**
     * Builds the JDBC URL for the database connection
     */
    public String buildJdbcUrl() {
        switch (type.toLowerCase()) {
            case "postgresql":
            case "postgres":
                return buildPostgresUrl();
            case "mysql":
                return buildMySqlUrl();
            case "oracle":
                return buildOracleUrl();
            case "sqlserver":
            case "mssql":
                return buildSqlServerUrl();
            case "h2":
                return buildH2Url();
            default:
                throw new IllegalArgumentException("Unsupported database type: " + type);
        }
    }
    
    private String buildPostgresUrl() {
        StringBuilder url = new StringBuilder("jdbc:postgresql://");
        url.append(host);
        if (port != null) {
            url.append(":").append(port);
        }
        url.append("/").append(database);
        
        if (ssl) {
            url.append("?sslmode=require");
        }
        
        return url.toString();
    }
    
    private String buildMySqlUrl() {
        StringBuilder url = new StringBuilder("jdbc:mysql://");
        url.append(host);
        if (port != null) {
            url.append(":").append(port);
        }
        url.append("/").append(database);
        
        if (ssl) {
            url.append("?useSSL=true");
        }
        
        return url.toString();
    }
    
    private String buildOracleUrl() {
        StringBuilder url = new StringBuilder("jdbc:oracle:thin:@");
        url.append(host);
        if (port != null) {
            url.append(":").append(port);
        }
        url.append(":").append(database);
        
        return url.toString();
    }
    
    private String buildSqlServerUrl() {
        StringBuilder url = new StringBuilder("jdbc:sqlserver://");
        url.append(host);
        if (port != null) {
            url.append(":").append(port);
        }
        url.append(";databaseName=").append(database);
        
        if (ssl) {
            url.append(";encrypt=true");
        }
        
        return url.toString();
    }
    
    private String buildH2Url() {
        if ("memory".equalsIgnoreCase(mode)) {
            return "jdbc:h2:mem:" + database + ";DB_CLOSE_DELAY=-1";
        } else {
            StringBuilder url = new StringBuilder("jdbc:h2:");
            if (file != null) {
                url.append(file);
            } else {
                url.append("./").append(database);
            }
            
            if (compression) {
                url.append(";COMPRESS=TRUE");
            }
            
            return url.toString();
        }
    }
    
    /**
     * Configuration for connection pooling
     */
    public static class ConnectionPoolConfig {
        @JsonProperty("maxConnections")
        private Integer maxConnections = 10;
        
        @JsonProperty("timeout")
        private Integer timeout = 30000;
        
        @JsonProperty("minIdle")
        private Integer minIdle = 2;
        
        public Integer getMaxConnections() {
            return maxConnections;
        }
        
        public void setMaxConnections(Integer maxConnections) {
            this.maxConnections = maxConnections;
        }
        
        public Integer getTimeout() {
            return timeout;
        }
        
        public void setTimeout(Integer timeout) {
            this.timeout = timeout;
        }
        
        public Integer getMinIdle() {
            return minIdle;
        }
        
        public void setMinIdle(Integer minIdle) {
            this.minIdle = minIdle;
        }
    }
    
    @Override
    public String toString() {
        return "DatabaseConfig{" +
                "type='" + type + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", database='" + database + '\'' +
                ", username='" + username + '\'' +
                ", file='" + file + '\'' +
                ", mode='" + mode + '\'' +
                ", ssl=" + ssl +
                '}';
    }
} 