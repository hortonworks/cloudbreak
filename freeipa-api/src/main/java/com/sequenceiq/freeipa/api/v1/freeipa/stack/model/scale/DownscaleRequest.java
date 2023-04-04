package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.validation.MutuallyExclusiveNotNull;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityType;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("FreeIpaDownscaleV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@MutuallyExclusiveNotNull(fieldGroups = {"targetAvailabilityType", "instanceIds"}, message = "Either targetAvailabilityType or instanceIds should be " +
        "provided but not both.")
public class DownscaleRequest extends ScaleRequestBase {

    @ApiModelProperty(value = ModelDescriptions.AVAILABILITY_TYPE)
    private AvailabilityType targetAvailabilityType;

    @ApiModelProperty(ModelDescriptions.INSTANCE_ID)
    private Set<String> instanceIds;

    public AvailabilityType getTargetAvailabilityType() {
        return targetAvailabilityType;
    }

    public void setTargetAvailabilityType(AvailabilityType targetAvailabilityType) {
        this.targetAvailabilityType = targetAvailabilityType;
    }

    public Set<String> getInstanceIds() {
        return instanceIds;
    }

    public void setInstanceIds(Set<String> instanceIds) {
        this.instanceIds = instanceIds;
    }

    @Override
    public String toString() {
        return "DownscaleRequest{" +
                "targetAvailabilityType=" + targetAvailabilityType +
                ", instanceIds=" + instanceIds +
                "} " + super.toString();
    }
}
