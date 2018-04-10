package com.sequenceiq.cloudbreak.cloud.model;


public class SupportedDatabaseEntry {

    private String databaseName;

    private String displayName;

    private String jdbcPrefix;

    public SupportedDatabaseEntry() {
    }

    public SupportedDatabaseEntry(String databaseName, String displayName, String jdbcPrefix) {
        this.databaseName = databaseName;
        this.displayName = displayName;
        this.jdbcPrefix = jdbcPrefix;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getJdbcPrefix() {
        return jdbcPrefix;
    }

    public void setJdbcPrefix(String jdbcPrefix) {
        this.jdbcPrefix = jdbcPrefix;
    }
}
