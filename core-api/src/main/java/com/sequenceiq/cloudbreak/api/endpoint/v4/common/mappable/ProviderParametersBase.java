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

    public abstract Mappable getAws();

    public abstract Mappable getGcp();

    public abstract Mappable getAzure();

    public abstract Mappable getOpenstack();

    public abstract Mappable getYarn();

    public abstract Mappable getMock();
}
