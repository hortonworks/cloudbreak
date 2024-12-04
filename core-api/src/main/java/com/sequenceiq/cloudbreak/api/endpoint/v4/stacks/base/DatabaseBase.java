package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base;

import static com.sequenceiq.cloudbreak.common.database.DatabaseCommon.POSTGRES_VERSION_REGEX;

import java.io.Serializable;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;

import io.swagger.v3.oas.annotations.media.Schema;

public abstract class DatabaseBase implements Serializable {

    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private DatabaseAvailabilityType availabilityType;

    private DatabaseAvailabilityType datalakeDatabaseAvailabilityType;

    @Pattern(regexp = POSTGRES_VERSION_REGEX, message = "Not a valid database major version")
    private String databaseEngineVersion;

    public DatabaseAvailabilityType getAvailabilityType() {
        return availabilityType;
    }

    public void setAvailabilityType(DatabaseAvailabilityType availabilityType) {
        this.availabilityType = availabilityType;
    }

    public String getDatabaseEngineVersion() {
        return databaseEngineVersion;
    }

    public void setDatabaseEngineVersion(String databaseEngineVersion) {
        this.databaseEngineVersion = databaseEngineVersion;
    }

    public DatabaseAvailabilityType getDatalakeDatabaseAvailabilityType() {
        return datalakeDatabaseAvailabilityType;
    }

    public void setDatalakeDatabaseAvailabilityType(DatabaseAvailabilityType datalakeDatabaseAvailabilityType) {
        this.datalakeDatabaseAvailabilityType = datalakeDatabaseAvailabilityType;
    }

    @Override
    public String toString() {
        return "DatabaseBase{" +
                "availabilityType=" + availabilityType +
                ", datalakeDatabaseAvailabilityType=" + datalakeDatabaseAvailabilityType +
                ", databaseEngineVersion='" + databaseEngineVersion + '\'' +
                '}';
    }
}
