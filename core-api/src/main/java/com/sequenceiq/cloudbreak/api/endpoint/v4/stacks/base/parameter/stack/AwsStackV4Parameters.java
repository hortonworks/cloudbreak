package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform.AWS;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;

import io.swagger.annotations.ApiModelProperty;

public class AwsStackV4Parameters extends StackV4ParameterBase {

    @Override
    @JsonIgnore
    @ApiModelProperty(hidden = true)
    public CloudPlatform getCloudPlatform() {
        return AWS;
    }
}
