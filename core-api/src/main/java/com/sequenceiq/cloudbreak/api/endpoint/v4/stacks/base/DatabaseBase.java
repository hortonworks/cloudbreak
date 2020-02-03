package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;

public abstract class DatabaseBase implements Serializable {

    @NotNull
    private DatabaseAvailabilityType availabilityType;

    public DatabaseAvailabilityType getAvailabilityType() {
        return availabilityType;
    }

    public void setAvailabilityType(DatabaseAvailabilityType availabilityType) {
        this.availabilityType = availabilityType;
    }
}
