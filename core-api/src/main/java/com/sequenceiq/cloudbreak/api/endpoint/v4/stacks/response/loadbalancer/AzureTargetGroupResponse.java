package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.loadbalancer;

import java.io.Serializable;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModelProperty;

public class AzureTargetGroupResponse implements Serializable {

    @ApiModelProperty(StackModelDescription.AZURE_LB_AVAILABILITY_SET)
    @NotNull
    private List<String> availabilitySet;

    public List<String> getAvailabilitySet() {
        return availabilitySet;
    }

    public void setAvailabilitySet(List<String> availabilitySet) {
        this.availabilitySet = availabilitySet;
    }
}
