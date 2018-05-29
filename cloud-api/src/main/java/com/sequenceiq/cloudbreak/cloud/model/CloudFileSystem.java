package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Map;

public class CloudFileSystem {

    private final Map<String, String> properties;

    public CloudFileSystem(Map<String, String> properties) {
        this.properties = properties;
    }

    public Map<String, String> getProperties() {
        return properties;
    }
}
