package com.sequenceiq.cloudbreak.model;

import java.util.HashMap;
import java.util.Map;

public class FileSystemConfiguration {

    public static final String STORAGE_CONTAINER = "container";
    public static final String RESOURCE_GROUP_NAME = "resourceGroupName";
    private Map<String, String> dynamicProperties = new HashMap<>();

    public String getProperty(String key) {
        return dynamicProperties.get(key);
    }

    public void addProperty(String key, String value) {
        this.dynamicProperties.put(key, value);
    }

}
