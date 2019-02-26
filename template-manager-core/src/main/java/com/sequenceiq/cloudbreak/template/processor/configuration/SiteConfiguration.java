package com.sequenceiq.cloudbreak.template.processor.configuration;

import java.util.HashMap;
import java.util.Map;

public class SiteConfiguration {
    private final String name;

    private final Map<String, String> properties;

    public SiteConfiguration(String name, Map<String, String> properties) {
        this.name = name;
        this.properties = properties;
    }

    public static SiteConfiguration getEmptyConfiguration(String name) {
        return new SiteConfiguration(name, new HashMap<>());
    }

    public String getName() {
        return name;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void add(String key, String value) {
        properties.put(key, value);
    }
}
