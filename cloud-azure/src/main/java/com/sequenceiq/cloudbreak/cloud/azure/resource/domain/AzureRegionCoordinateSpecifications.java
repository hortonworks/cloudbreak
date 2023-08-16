package com.sequenceiq.cloudbreak.cloud.azure.resource.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AzureRegionCoordinateSpecifications {
    @JsonProperty("items")
    private List<AzureRegionCoordinateSpecification> items;

    public List<AzureRegionCoordinateSpecification> getItems() {
        return items;
    }

    public void setItems(List<AzureRegionCoordinateSpecification> items) {
        this.items = items;
    }
}

