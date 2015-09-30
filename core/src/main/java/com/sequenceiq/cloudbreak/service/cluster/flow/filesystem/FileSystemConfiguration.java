package com.sequenceiq.cloudbreak.service.cluster.flow.filesystem;

import java.util.HashMap;
import java.util.Map;

public class FileSystemConfiguration {

    public static final String STORAGE_CONTAINER = "container";
    private Map<String, String> dynamicProperties = new HashMap<>();

    public String getProperty(String key) {
        return dynamicProperties.get(key);
    }

    public void addProperty(String key, String value) {
        this.dynamicProperties.put(key, value);
    }

}
