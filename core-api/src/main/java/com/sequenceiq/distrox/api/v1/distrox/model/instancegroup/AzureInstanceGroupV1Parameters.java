package com.sequenceiq.distrox.api.v1.distrox.model.instancegroup;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AzureInstanceGroupV1Parameters implements Serializable {

    @Schema
    private AzureAvailabiltySetV1Parameters availabilitySet;

    public AzureAvailabiltySetV1Parameters getAvailabilitySet() {
        return availabilitySet;
    }

    public void setAvailabilitySet(AzureAvailabiltySetV1Parameters availabilitySet) {
        this.availabilitySet = availabilitySet;
    }
}
