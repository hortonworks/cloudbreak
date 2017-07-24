package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.Map;

public class CloudSshKey {

    private String name;

    private Map<String, Object> properties = new HashMap<>();

    public CloudSshKey() {
    }

    public CloudSshKey(String name, Map<String, Object> properties) {

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
