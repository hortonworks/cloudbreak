package com.sequenceiq.cloudbreak.validation.externaldatabase;


import java.util.Set;

public class SupportedDatabaseEntry {

    private String databaseName;

    private String displayName;

    private String jdbcPrefix;

    private Set<String> versions;

    public SupportedDatabaseEntry() {
    }

    public SupportedDatabaseEntry(String databaseName, String displayName, String jdbcPrefix, Set<String> versions) {
        this.databaseName = databaseName;
        this.displayName = displayName;
        this.jdbcPrefix = jdbcPrefix;
        this.versions = versions;
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

    public Set<String> getVersions() {
        return versions;
    }

    public void setVersions(Set<String> versions) {
        this.versions = versions;
    }
}
