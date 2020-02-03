package com.sequenceiq.distrox.api.v1.distrox.model.database;

import javax.validation.constraints.NotNull;

public abstract class DistroXDatabaseBase {

    @NotNull
    private DistroXDatabaseAvailabilityType availabilityType;

    public DistroXDatabaseAvailabilityType getAvailabilityType() {
        return availabilityType;
    }

    public void setAvailabilityType(DistroXDatabaseAvailabilityType availabilityType) {
        this.availabilityType = availabilityType;
    }
}
