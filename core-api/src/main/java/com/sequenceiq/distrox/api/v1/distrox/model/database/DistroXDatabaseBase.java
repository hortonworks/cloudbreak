package com.sequenceiq.distrox.api.v1.distrox.model.database;

import static com.sequenceiq.cloudbreak.common.database.DatabaseCommon.POSTGRES_VERSION_REGEX;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

public abstract class DistroXDatabaseBase {

    @NotNull
    private DistroXDatabaseAvailabilityType availabilityType;

    @Pattern(regexp = POSTGRES_VERSION_REGEX, message = "Not a valid database major version")
    private String databaseEngineVersion;

    public DistroXDatabaseAvailabilityType getAvailabilityType() {
        return availabilityType;
    }

    public void setAvailabilityType(DistroXDatabaseAvailabilityType availabilityType) {
        this.availabilityType = availabilityType;
    }

    public String getDatabaseEngineVersion() {
        return databaseEngineVersion;
    }

    public void setDatabaseEngineVersion(String databaseEngineVersion) {
        this.databaseEngineVersion = databaseEngineVersion;
    }
}
