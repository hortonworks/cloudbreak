package com.sequenceiq.cloudbreak.api.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformSshKeyResponse implements JsonEntity {

    private String name;

    private Map<String, Object> properties = new HashMap<>();

    public PlatformSshKeyResponse() {
    }

    public PlatformSshKeyResponse(String name, Map<String, Object> properties) {
        this.name = name;
        this.properties = properties;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

}
