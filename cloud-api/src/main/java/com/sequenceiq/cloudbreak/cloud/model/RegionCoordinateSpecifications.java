package com.sequenceiq.cloudbreak.cloud.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RegionCoordinateSpecifications {
    @JsonProperty("items")
    private List<RegionCoordinateSpecification> items;

    public List<RegionCoordinateSpecification> getItems() {
        return items;
    }

    public void setItems(List<RegionCoordinateSpecification> items) {
        this.items = items;
    }
}
