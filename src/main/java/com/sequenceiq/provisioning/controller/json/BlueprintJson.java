package com.sequenceiq.provisioning.controller.json;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;

public class BlueprintJson {

    private String ambariBlueprint;

    @JsonRawValue
    public String getAmbariBlueprint() {
        return ambariBlueprint;
    }

    public void setAmbariBlueprint(JsonNode node) {
        this.ambariBlueprint = node.toString();
    }

}
