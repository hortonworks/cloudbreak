package com.sequenceiq.cloudbreak.structuredevent.event;

import com.fasterxml.jackson.databind.JsonNode;

public class BlueprintDetails {
    private Long id;

    private String name;

    private String description;

    private String blueprintName;

    private JsonNode blueprintJson;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBlueprintName() {
        return blueprintName;
    }

    public void setBlueprintName(String blueprintName) {
        this.blueprintName = blueprintName;
    }

    public JsonNode getBlueprintJson() {
        return blueprintJson;
    }

    public void setBlueprintJson(JsonNode blueprintJson) {
        this.blueprintJson = blueprintJson;
    }
}
