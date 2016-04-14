package com.sequenceiq.cloudbreak.cloud.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RegionsSpecification {
    @JsonProperty("items")
    private List<RegionSpecification> items;

    public RegionsSpecification() {
    }

    public List<RegionSpecification> getItems() {
        return items;
    }

    public void setItems(List<RegionSpecification> items) {
        this.items = items;
    }
}
