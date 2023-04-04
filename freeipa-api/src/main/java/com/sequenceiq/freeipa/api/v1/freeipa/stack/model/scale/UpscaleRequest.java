package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityType;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("FreeIpaUpscaleV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpscaleRequest extends ScaleRequestBase {

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.AVAILABILITY_TYPE, required = true)
    private AvailabilityType targetAvailabilityType;

    public AvailabilityType getTargetAvailabilityType() {
        return targetAvailabilityType;
    }

    public void setTargetAvailabilityType(AvailabilityType targetAvailabilityType) {
        this.targetAvailabilityType = targetAvailabilityType;
    }

    @Override
    public String toString() {
        return "UpscaleRequest{" +
                "targetAvailabilityType=" + targetAvailabilityType +
                "} " + super.toString();
    }
}
