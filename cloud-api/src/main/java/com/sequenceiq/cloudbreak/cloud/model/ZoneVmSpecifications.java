package com.sequenceiq.cloudbreak.cloud.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ZoneVmSpecifications {
    @JsonProperty("items")
    private List<ZoneVmSpecification> items;

    public List<ZoneVmSpecification> getItems() {
        return items;
    }

    public void setItems(List<ZoneVmSpecification> items) {
        this.items = items;
    }
}
