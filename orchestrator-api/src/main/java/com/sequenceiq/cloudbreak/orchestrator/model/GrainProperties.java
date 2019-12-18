package com.sequenceiq.cloudbreak.orchestrator.model;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class GrainProperties {

    private final Map<String, Map<String, String>> properties = new HashMap<>();

    public Map<String, String> computeIfAbsent(String key, Function<? super String, ? extends Map<String, String>> mappingFunction) {
        return properties.computeIfAbsent(key, mappingFunction);
    }

    public Map<String, String> put(String key, Map<String, String> value) {
        return properties.put(key, value);
    }

    public Map<String, Map<String, String>> getProperties() {
        return new HashMap<>(properties);
    }
}
