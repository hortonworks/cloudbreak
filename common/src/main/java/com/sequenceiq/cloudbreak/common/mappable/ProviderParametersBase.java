package com.sequenceiq.cloudbreak.common.mappable;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;

public abstract class ProviderParametersBase {

    @JsonIgnore
    @Schema(hidden = true)
    private CloudPlatform cloudPlatform;

    public CloudPlatform getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(CloudPlatform cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public abstract Mappable createAws();

    public abstract Mappable createGcp();

    public abstract Mappable createAzure();

    public abstract Mappable createYarn();

    public abstract Mappable createMock();

}
