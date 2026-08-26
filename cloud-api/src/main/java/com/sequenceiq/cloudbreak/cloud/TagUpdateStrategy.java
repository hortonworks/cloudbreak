package com.sequenceiq.cloudbreak.cloud;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

public interface TagUpdateStrategy {

    Set<ResourceType> supportedTypes();

    void updateTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Map<String, String> tags) throws IOException;

    void deleteTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Set<String> tagKeys) throws IOException;

    default boolean isBatchUpdateSupported() {
        return false;
    }

    default void batchUpdateTags(AuthenticatedContext authenticatedContext, List<CloudResource> cloudResources, Map<String, String> tags) {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

    default boolean tagsAlreadyUpToDate(Map<String, String> existingTags, Map<String, String> newTags) {
        Map<String, String> existing = existingTags != null ? existingTags : Map.of();
        return existing.entrySet().containsAll(newTags.entrySet());
    }

    default Map<String, String> mergeTags(Map<String, String> existingTags, Map<String, String> newTags) {
        Map<String, String> merged = existingTags != null ? new HashMap<>(existingTags) : new HashMap<>();
        merged.putAll(newTags);
        return merged;
    }

    default Map<String, String> removeTagKeys(Map<String, String> existingTags, Set<String> tagKeys) {
        Map<String, String> updated = existingTags != null ? new HashMap<>(existingTags) : new HashMap<>();
        tagKeys.forEach(updated::remove);
        return updated;
    }

    default void logTagKeyDeletion(Logger logger, String targetDescription, Set<String> tagKeys) {
        logger.info("Deleting tag keys {} from {}", tagKeys, targetDescription);
    }

    default void logTagDeletion(Logger logger, String resourceIdentifier, Set<String> requestedTagKeys,
        Map<String, String> existingTags, Set<String> remainingKeys) {
        Set<String> deletingKeys = tagKeysToDelete(existingTags, requestedTagKeys);
        logger.info("Deleting tag keys {} from {}, remaining tag keys: {}", deletingKeys, resourceIdentifier, remainingKeys);
    }

    private Set<String> tagKeysToDelete(Map<String, String> existingTags, Set<String> tagKeys) {
        Map<String, String> existing = existingTags != null ? existingTags : Map.of();
        return tagKeys.stream()
                .filter(existing::containsKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    default boolean hasTagKeysToDelete(Map<String, String> existingTags, Set<String> tagKeys) {
        Map<String, String> existing = existingTags != null ? existingTags : Map.of();
        return tagKeys.stream().anyMatch(existing::containsKey);
    }
}
