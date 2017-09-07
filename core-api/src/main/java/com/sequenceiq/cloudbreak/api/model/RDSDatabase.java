package com.sequenceiq.cloudbreak.api.model;

public enum RDSDatabase {
    POSTGRES("postgres", "org.postgresql.Driver", "Existing PostgreSQL Database");

    private final String dbName;

    private final String dbDriver;

    private final String ambariDbOption;

    RDSDatabase(String dbName, String dbDriver, String ambariDbOption) {
        this.dbName = dbName;
        this.dbDriver = dbDriver;
        this.ambariDbOption = ambariDbOption;
    }

    public String getDbName() {
        return dbName;
    }

    public String getDbDriver() {
        return dbDriver;
    }

    public String getAmbariDbOption() {
        return ambariDbOption;
    }
}
