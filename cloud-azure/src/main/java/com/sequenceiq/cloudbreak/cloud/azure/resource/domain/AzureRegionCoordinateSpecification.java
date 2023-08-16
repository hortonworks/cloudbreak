package com.sequenceiq.cloudbreak.cloud.azure.resource.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.RegionCoordinateSpecification;

public class AzureRegionCoordinateSpecification extends RegionCoordinateSpecification {

    @JsonProperty("flexible")
    private AzureFlexibleRegionCoordinateSpecification flexible;

    public AzureFlexibleRegionCoordinateSpecification getFlexible() {
        return flexible;
    }

    public void setFlexible(AzureFlexibleRegionCoordinateSpecification flexible) {
        this.flexible = flexible;
    }
}
