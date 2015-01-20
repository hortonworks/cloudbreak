package com.sequenceiq.it;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(locations = "itcontext.properties")
public class IntegrationTestContextProperties {
    private Map<String, Object> propertiesMap = new HashMap<>();

    public Map<String, Object> getPropertiesMap() {
        return propertiesMap;
    }
}
