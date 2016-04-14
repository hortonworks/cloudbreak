package com.sequenceiq.cloudbreak.cloud.gcp.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ZoneDefinitionWrapper {

    @JsonProperty("id")
    private String id;
    @JsonProperty("kind")
    private String kind;
    @JsonProperty("selfLink")
    private String selfLink;
    @JsonProperty("items")
    private List<ZoneDefinitionView> items;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getSelfLink() {
        return selfLink;
    }

    public void setSelfLink(String selfLink) {
        this.selfLink = selfLink;
    }

    public List<ZoneDefinitionView> getItems() {
        return items;
    }

    public void setItems(List<ZoneDefinitionView> items) {
        this.items = items;
    }
}
