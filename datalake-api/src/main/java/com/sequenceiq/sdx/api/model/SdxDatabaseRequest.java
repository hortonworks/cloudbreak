package com.sequenceiq.sdx.api.model;

public class SdxDatabaseRequest {

    private Boolean create;

    private SdxDatabaseAvailabilityType availabilityType;

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
}
