package com.db2h2.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to track migration progress and timing
 */
public class ProgressTracker {
    
    private static final Logger logger = LoggerFactory.getLogger(ProgressTracker.class);
    
    private long startTime;
    private long endTime;
    private boolean isRunning;
    private int totalTables;
    private int completedTables;
    private int totalRows;
    private int completedRows;
    
    public ProgressTracker() {
        this.isRunning = false;
        this.completedTables = 0;
        this.completedRows = 0;
    }
    
    /**
     * Starts the progress tracking
     */
    public void start() {
        this.startTime = System.currentTimeMillis();
        this.isRunning = true;
        this.completedTables = 0;
        this.completedRows = 0;
        logger.info("Progress tracking started");
    }
    
    /**
     * Completes the progress tracking
     */
    public void complete() {
        this.endTime = System.currentTimeMillis();
        this.isRunning = false;
        logger.info("Progress tracking completed. Total duration: {} ms", getDuration());
    }
    
    /**
     * Sets the total number of tables to migrate
     */
    public void setTotalTables(int totalTables) {
        this.totalTables = totalTables;
        logger.info("Total tables to migrate: {}", totalTables);
    }
    
    /**
     * Increments the completed tables counter
     */
    public void incrementCompletedTables() {
        this.completedTables++;
        logProgress();
    }
    
    /**
     * Sets the total number of rows to migrate
     */
    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
        logger.info("Total rows to migrate: {}", totalRows);
    }
    
    /**
     * Increments the completed rows counter
     */
    public void incrementCompletedRows(int count) {
        this.completedRows += count;
        logProgress();
    }
    
    /**
     * Gets the current duration in milliseconds
     */
    public long getDuration() {
        if (isRunning) {
            return System.currentTimeMillis() - startTime;
        } else {
            return endTime - startTime;
        }
    }
    
    /**
     * Gets the progress percentage for tables
     */
    public double getTableProgressPercentage() {
        if (totalTables == 0) {
            return 0.0;
        }
        return (double) completedTables / totalTables * 100.0;
    }
    
    /**
     * Gets the progress percentage for rows
     */
    public double getRowProgressPercentage() {
        if (totalRows == 0) {
            return 0.0;
        }
        return (double) completedRows / totalRows * 100.0;
    }
    
    /**
     * Logs the current progress
     */
    private void logProgress() {
        if (totalTables > 0) {
            double tableProgress = getTableProgressPercentage();
            double rowProgress = getRowProgressPercentage();
            
            logger.info("Progress: {}/{} tables ({:.1f}%), {}/{} rows ({:.1f}%)", 
                       completedTables, totalTables, tableProgress,
                       completedRows, totalRows, rowProgress);
        }
    }
    
    /**
     * Gets a formatted duration string
     */
    public String getFormattedDuration() {
        long duration = getDuration();
        long seconds = duration / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    // Getters
    public boolean isRunning() { return isRunning; }
    public int getTotalTables() { return totalTables; }
    public int getCompletedTables() { return completedTables; }
    public int getTotalRows() { return totalRows; }
    public int getCompletedRows() { return completedRows; }
} 