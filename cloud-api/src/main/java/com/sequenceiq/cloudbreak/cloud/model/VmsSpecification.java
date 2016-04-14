package com.sequenceiq.cloudbreak.cloud.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VmsSpecification {
    @JsonProperty("items")
    private List<VmSpecification> items;

    public VmsSpecification() {
    }

    public List<VmSpecification> getItems() {
        return items;
    }

    public void setItems(List<VmSpecification> items) {
        this.items = items;
    }
}
