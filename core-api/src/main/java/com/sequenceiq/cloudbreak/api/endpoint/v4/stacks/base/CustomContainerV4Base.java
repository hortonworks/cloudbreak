package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomContainerV4Base implements JsonEntity {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, String> definitions = new HashMap<>();

    public Map<String, String> getDefinitions() {
        return definitions;
    }

    public void setDefinitions(Map<String, String> definitions) {
        this.definitions = definitions;
    }
}
