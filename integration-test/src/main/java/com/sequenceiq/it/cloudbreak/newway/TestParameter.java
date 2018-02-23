package com.sequenceiq.it.cloudbreak.newway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class TestParameter {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestParameter.class);

    private final Map<String, String> parameters;

    TestParameter() {
        parameters = new HashMap<>();
    }

    public String get(String key) {
        String valueAsProperty = parameters.get(key);
        if (valueAsProperty == null) {
            LOGGER.info("key has not been found as property, trying as environment variable");
            valueAsProperty = parameters.get(key.toUpperCase().replaceAll("\\.", "_"));
        }
        LOGGER.info("Aquiring key {} resulting: {}", key, valueAsProperty);

        return valueAsProperty;
    }

    public void put(String key, String value) {
        parameters.put(key, value);
    }

    public void putAll(Map<String, String> all) {
        parameters.putAll(all);
    }

    public int size() {
        return parameters.size();
    }
}
