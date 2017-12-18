package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.Map;

public class CloudIpPool {

    private String name;

    private String id;

    private Map<String, Object> properties = new HashMap<>();

    public CloudIpPool() {
    }

    public CloudIpPool(String name, String id, Map<String, Object> properties) {
        this.name = name;
        this.id = id;
        this.properties = properties;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    @Override
    public String toString() {
        return "CloudIpPool{"
                + "name='" + name + '\''
                + ", id='" + id + '\''
                + ", properties=" + properties
                + '}';
    }
}
