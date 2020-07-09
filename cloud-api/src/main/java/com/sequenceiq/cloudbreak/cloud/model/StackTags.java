package com.sequenceiq.cloudbreak.cloud.model;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.common.api.tag.model.Tags;

@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class StackTags {

    private final Tags userDefinedTags;

    private final Tags applicationTags;

    private final Tags defaultTags;

    public StackTags(@JsonProperty("userDefinedTags") Tags userDefinedTags,
            @JsonProperty("applicationTags") Tags applicationTags,
            @JsonProperty("defaultTags") Tags defaultTags) {
        this.defaultTags = Objects.requireNonNullElse(defaultTags, new Tags());
        this.applicationTags = Objects.requireNonNullElse(applicationTags, new Tags());
        this.userDefinedTags = Objects.requireNonNullElse(userDefinedTags, new Tags());
    }

    public Tags getUserDefinedTags() {
        return userDefinedTags;
    }

    public Tags getApplicationTags() {
        return applicationTags;
    }

    public Tags getDefaultTags() {
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
