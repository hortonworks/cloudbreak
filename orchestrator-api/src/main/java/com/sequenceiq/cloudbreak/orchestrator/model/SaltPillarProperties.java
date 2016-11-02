package com.sequenceiq.cloudbreak.orchestrator.model;

import java.util.Map;

public class SaltPillarProperties {

    private String path;

    private Map<String, Object> properties;

    public SaltPillarProperties(String path, Map<String, Object> properties) {
        this.path = path;
        this.properties = properties;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
}
