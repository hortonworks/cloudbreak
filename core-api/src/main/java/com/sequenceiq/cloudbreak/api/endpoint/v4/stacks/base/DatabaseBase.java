package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base;

import static com.sequenceiq.cloudbreak.common.database.DatabaseCommon.POSTGRES_VERSION_REGEX;

import java.io.Serializable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;

public abstract class DatabaseBase implements Serializable {

    @NotNull
    private DatabaseAvailabilityType availabilityType;

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
}
