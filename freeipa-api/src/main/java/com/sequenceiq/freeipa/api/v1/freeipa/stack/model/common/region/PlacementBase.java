package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.region;

import javax.validation.constraints.NotNull;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions;

import io.swagger.annotations.ApiModelProperty;

public abstract class PlacementBase {
    @ApiModelProperty(FreeIpaModelDescriptions.AVAILABILITY_ZONE)
    private String availabilityZone;

    @NotNull
    @ApiModelProperty(FreeIpaModelDescriptions.REGION)
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
}
