package com.db2h2.connectors;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.db2h2.schema.TableMetadata;

/**
 * Interface for database connectors
 */
public interface DatabaseConnector {
    
    /**
     * Connects to the database
     */
    void connect() throws SQLException;
    
    /**
     * Disconnects from the database
     */
    void disconnect();
    
    /**
     * Checks if connected to the database
     */
    boolean isConnected();
    
    /**
     * Gets the underlying JDBC connection
     */
    Connection getConnection();
    
    /**
     * Gets all table names from the database
     */
    List<String> getTableNames() throws SQLException;
    
    /**
     * Gets metadata for a specific table
     */
    TableMetadata getTableMetadata(String tableName) throws SQLException;
    
    /**
     * Executes a query and returns the result set
     */
    ResultSet executeQuery(String sql) throws SQLException;
    
    /**
     * Executes an update statement
     */
    int executeUpdate(String sql) throws SQLException;
    
    /**
     * Gets the row count for a table
     */
    long getRowCount(String tableName) throws SQLException;
    
    /**
     * Gets data from a table with limit and offset
     */
    ResultSet getTableData(String tableName, int limit, int offset) throws SQLException;
    
    /**
     * Gets the database type
     */
    String getDatabaseType();
    
    /**
     * Gets the database version
     */
    String getDatabaseVersion() throws SQLException;
    
} 