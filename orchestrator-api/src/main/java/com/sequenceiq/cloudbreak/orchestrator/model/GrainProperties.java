package com.sequenceiq.cloudbreak.orchestrator.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.google.common.collect.ArrayListMultimap;

public class GrainProperties {

    private final Map<String, Map<String, String>> properties = new HashMap<>();

    public Map<String, String> computeIfAbsent(String key, Function<? super String, ? extends Map<String, String>> mappingFunction) {
        return properties.computeIfAbsent(key, mappingFunction);
    }

    public Map<String, String> put(String key, Map<String, String> value) {
        return properties.put(key, value);
    }

    /**
     * @return Map<FQDN, Map<GrainKey, GrainValue>>
     */
    public Map<String, Map<String, String>> getProperties() {
        return new HashMap<>(properties);
    }

    /**
     * @return Map<Map<GrainKey, GrainValue>, Collection<FQDN>>
     */
    public Map<Map.Entry<String, String>, Collection<String>> getHostsPerGrainMap() {
        ArrayListMultimap<Map.Entry<String, String>, String> multimap = ArrayListMultimap.create();
        getProperties().forEach((host, grainMap) -> grainMap.entrySet().forEach(grain -> multimap.put(grain, host)));
        return multimap.asMap();
    }
}
