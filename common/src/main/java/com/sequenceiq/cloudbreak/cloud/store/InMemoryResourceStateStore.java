package com.sequenceiq.cloudbreak.cloud.store;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;

public class InMemoryResourceStateStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryResourceStateStore.class);

    private static final Map<String, Map<Long, PollGroup>> RESOURCE_STATE_STORE = new ConcurrentHashMap<>();

    private InMemoryResourceStateStore() {
    }

    public static PollGroup getResource(String resourceType, Long resourceId) {
        return RESOURCE_STATE_STORE.getOrDefault(resourceType, Collections.emptyMap()).get(resourceId);
    }

    public static Set<String> getResourceTypes() {
        return RESOURCE_STATE_STORE.keySet();
    }

    public static Set<Long> getAllResourceId(String resourceType) {
        return RESOURCE_STATE_STORE.getOrDefault(resourceType, Collections.emptyMap()).keySet();
    }

    public static void putResource(String resourceType, Long resourceId, PollGroup value) {
        Map<Long, PollGroup> resourceMap = RESOURCE_STATE_STORE.putIfAbsent(resourceType, new ConcurrentHashMap<>());
        if (resourceMap == null) {
            resourceMap = RESOURCE_STATE_STORE.get(resourceType);
        }
        resourceMap.put(resourceId, value);
        LOGGER.debug("The polling inited in the in-memory-store for {} with {}: {}", resourceType, resourceId, value);
    }

    public static void deleteResource(String resourceType, Long resourceId) {
        RESOURCE_STATE_STORE.getOrDefault(resourceType, Collections.emptyMap()).remove(resourceId);
    }
}
