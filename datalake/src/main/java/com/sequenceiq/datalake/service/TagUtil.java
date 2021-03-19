package com.sequenceiq.datalake.service;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.common.json.Json;

public class TagUtil {

    private TagUtil() {

    }

    public static Map<String, String> getTags(Json tag) {
        try {
            Map<String, String> tags;
            if (tag != null && tag.getValue() != null) {
                tags = tag.get(Map.class);
            } else {
                tags = new HashMap<>();
            }
            return tags;
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot convert tags", e);
        }
    }
}
