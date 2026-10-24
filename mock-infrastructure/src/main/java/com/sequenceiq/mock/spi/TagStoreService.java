package com.sequenceiq.mock.spi;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TagStoreService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TagStoreService.class);

    private final Map<String, Map<String, String>> tagsByMockUuid = new ConcurrentHashMap<>();

    public void init(String mockUuid, Map<String, String> tags) {
        LOGGER.info("Initialize tags for {}: {}", mockUuid, tags);
        tagsByMockUuid.put(mockUuid, tags == null ? new HashMap<>() : new HashMap<>(tags));
    }

    public void update(String mockUuid, Map<String, String> tags) {
        LOGGER.info("Update tags for {}: {}", mockUuid, tags);
        tagsByMockUuid.compute(mockUuid, (key, existing) -> {
            Map<String, String> updated = existing == null ? new HashMap<>() : new HashMap<>(existing);
            updated.putAll(tags);
            return updated;
        });
    }

    public void delete(String mockUuid, Set<String> tagKeys) {
        LOGGER.info("Delete tag keys for {}: {}", mockUuid, tagKeys);
        tagsByMockUuid.computeIfPresent(mockUuid, (key, existing) -> {
            Map<String, String> updated = new HashMap<>(existing);
            tagKeys.forEach(updated::remove);
            return updated;
        });
    }

    public Map<String, String> get(String mockUuid) {
        return Map.copyOf(tagsByMockUuid.getOrDefault(mockUuid, Map.of()));
    }

    public void remove(String mockUuid) {
        LOGGER.info("Remove tags of {}", mockUuid);
        tagsByMockUuid.remove(mockUuid);
    }
}
