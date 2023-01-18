package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class YarnInstanceGroupV4Parameters extends InstanceGroupV4ParametersBase {

    @JsonIgnore
    @Schema(hidden = true)
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.YARN;
    }
}
