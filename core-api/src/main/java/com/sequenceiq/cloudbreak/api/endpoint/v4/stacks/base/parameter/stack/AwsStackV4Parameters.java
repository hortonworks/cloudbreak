package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Map;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AwsStackV4Parameters extends StackV4ParameterBase {
    @ApiModelProperty
    private boolean createCloudwatch;

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = super.asMap();
        putIfValueNotNull(map, PlatformParametersConsts.CLOUDWATCH_CREATE_PARAMETER, Boolean.toString(createCloudwatch));
        return map;
    }

    @Override
    @JsonIgnore
    @ApiModelProperty(hidden = true)
    public CloudPlatform getCloudPlatform() {
        return AWS;
    }

    public boolean getCreateCloudwatch() {
        return createCloudwatch;
    }

    public void setCreateCloudwatch(boolean createCloudwatch) {
        this.createCloudwatch = createCloudwatch;
    }
}
