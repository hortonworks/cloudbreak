package com.sequenceiq.cloudbreak.cloud.gcp.tag;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.TagUpdateStrategy;

public interface GcpResourceTagUpdateStrategy extends TagUpdateStrategy {

    default Map<String, String> mergeLabels(Map<String, String> existingLabels, Map<String, String> newLabels) {
        Map<String, String> merged = new HashMap<>();
        if (existingLabels != null) {
            merged.putAll(existingLabels);
        }
        merged.putAll(newLabels);
        return merged;
    }
}
