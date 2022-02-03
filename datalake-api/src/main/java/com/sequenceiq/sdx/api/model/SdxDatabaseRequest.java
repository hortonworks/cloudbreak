package com.sequenceiq.sdx.api.model;

import static com.sequenceiq.cloudbreak.common.database.DatabaseCommon.POSTGRES_VERSION_REGEX;

import javax.validation.constraints.Pattern;

public class SdxDatabaseRequest {

    private Boolean create;

    private SdxDatabaseAvailabilityType availabilityType;

    @Pattern(regexp = POSTGRES_VERSION_REGEX, message = "Not a valid database major version")
    private String databaseEngineVersion;

    public Boolean getCreate() {
        return create;
    }

    public void setCreate(Boolean create) {
        this.create = create;
    }

    public SdxDatabaseAvailabilityType getAvailabilityType() {
        return availabilityType;
    }

    public void setAvailabilityType(SdxDatabaseAvailabilityType availabilityType) {
        this.availabilityType = availabilityType;
    }

    public String getDatabaseEngineVersion() {
        return databaseEngineVersion;
    }

    public void setDatabaseEngineVersion(String databaseEngineVersion) {
        this.databaseEngineVersion = databaseEngineVersion;
    }
}
