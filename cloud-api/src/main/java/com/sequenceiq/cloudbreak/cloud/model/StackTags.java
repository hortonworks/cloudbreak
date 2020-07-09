package com.sequenceiq.cloudbreak.cloud.model;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

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
        this.applicationTags = applicationTags;
        this.userDefinedTags = userDefinedTags;
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

    @Override
    public String toString() {
        return "StackTags{"
                + "userDefinedTags=" + userDefinedTags
                + ", applicationTags=" + applicationTags
                + ", defaultTags=" + defaultTags
                + '}';
    }
}
