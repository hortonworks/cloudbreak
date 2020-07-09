package com.sequenceiq.environment.environment.domain;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.common.api.tag.model.Tags;

@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnvironmentTags {

    private final Tags userDefinedTags;

    private final Tags defaultTags;

    public EnvironmentTags(@JsonProperty("userDefinedTags") Tags userDefinedTags,
        @JsonProperty("defaultTags") Tags defaultTags) {
        this.defaultTags = defaultTags;
        this.userDefinedTags = userDefinedTags;
    }

    public Tags getUserDefinedTags() {
        return userDefinedTags;
    }

    public Tags getDefaultTags() {
        return defaultTags;
    }

    @Override
    public String toString() {
        return "StackTags{"
                + "userDefinedTags=" + userDefinedTags
                + ", defaultTags=" + defaultTags
                + '}';
    }
}
