package com.sequenceiq.cloudbreak.cloud;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

public interface TagUpdateStrategy {

    Set<ResourceType> supportedTypes();

    void updateTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Map<String, String> tags) throws IOException;

    default boolean tagsAlreadyUpToDate(Map<String, String> existingTags, Map<String, String> newTags) {
        Map<String, String> existing = existingTags != null ? existingTags : Map.of();
        return existing.entrySet().containsAll(newTags.entrySet());
    }

    default Map<String, String> mergeTags(Map<String, String> existingTags, Map<String, String> newTags) {
        Map<String, String> merged = existingTags != null ? new HashMap<>(existingTags) : new HashMap<>();
        merged.putAll(newTags);
        return merged;
    }
}
