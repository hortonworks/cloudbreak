package com.sequenceiq.cloudbreak.cloud.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RegionDisplayNameSpecifications {
    @JsonProperty("items")
    private List<RegionDisplayNameSpecification> items;

    public List<RegionDisplayNameSpecification> getItems() {
        return items;
    }

    public void setItems(List<RegionDisplayNameSpecification> items) {
        this.items = items;
    }
}
