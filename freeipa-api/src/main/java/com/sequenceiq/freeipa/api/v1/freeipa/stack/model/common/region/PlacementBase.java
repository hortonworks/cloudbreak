package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.region;

import jakarta.validation.constraints.NotNull;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

public abstract class PlacementBase {
    @Schema(description = FreeIpaModelDescriptions.AVAILABILITY_ZONE)
    private String availabilityZone;

    @NotNull
    @Schema(description = FreeIpaModelDescriptions.REGION, requiredMode = Schema.RequiredMode.REQUIRED)
    private String region;

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    @Override
    public String toString() {
        return "PlacementBase{"
                + "availabilityZone='" + availabilityZone + '\''
                + ", region='" + region + '\''
                + '}';
    }
}
