package com.sequenceiq.cloudbreak.cloud.model;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;

@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class StackTags {

    private final Map<String, String> userDefinedTags;

    private final Map<String, String> applicationTags;

    private final Map<String, String> defaultTags;

    public StackTags(@JsonProperty("userDefinedTags") Map<String, String> userDefinedTags,
            @JsonProperty("applicationTags") Map<String, String> applicationTags,
            @JsonProperty("defaultTags") Map<String, String> defaultTags) {
        this.defaultTags = defaultTags;
        this.applicationTags = applicationTags != null ? applicationTags : Maps.newHashMap();
        this.userDefinedTags = userDefinedTags != null ? userDefinedTags : Maps.newHashMap();
    }

    public Map<String, String> getUserDefinedTags() {
        return userDefinedTags;
    }

    public Map<String, String> getApplicationTags() {
        return applicationTags;
    }

    public Map<String, String> getDefaultTags() {
        return defaultTags;
    }

    public void updateUserDefinedTags(Map<String, String> tags) {
        if (tags != null) {
            userDefinedTags.putAll(tags);
        }
    }

    public Map<String, String> getUserDefinedTagsWithoutDefaultTags(Map<String, String> userDefinedTags) {
        if (userDefinedTags == null) {
            return Map.of();
        }
        Map<String, String> result = new HashMap<>(userDefinedTags);
        if (defaultTags != null) {
            result.keySet().removeAll(defaultTags.keySet());
        }
        return result;
    }

    @Override
    public String toString() {
        return "StackTags{"
                + "userDefinedTags=" + userDefinedTags
                + ", applicationTags=" + applicationTags
                + ", defaultTags=" + defaultTags
                + '}';
    }
}
