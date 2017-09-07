package com.sequenceiq.cloudbreak.api.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomContainerResponse implements JsonEntity {

    private Map<String, String> definitions = new HashMap<>();

    public CustomContainerResponse() {
    }

    public CustomContainerResponse(Map<String, String> definitions) {
        this.definitions = definitions;
    }

    public Map<String, String> getDefinitions() {
        return definitions;
    }

    public void setDefinitions(Map<String, String> definitions) {
        this.definitions = definitions;
    }
}
