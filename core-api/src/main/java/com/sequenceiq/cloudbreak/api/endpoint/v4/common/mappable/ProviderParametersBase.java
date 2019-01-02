package com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable;

import io.swagger.annotations.ApiModelProperty;

public abstract class ProviderParametersBase {

    @ApiModelProperty(hidden = true)
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

    public abstract Mappable createOpenstack();

    public abstract Mappable createYarn();

    public abstract Mappable createMock();
}
