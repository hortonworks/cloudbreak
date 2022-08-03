package com.sequenceiq.environment.environment.domain;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnvironmentTags {

    private final Map<String, String> userDefinedTags;

    private final Map<String, String> defaultTags;

    @JsonCreator
    public EnvironmentTags(@JsonProperty("userDefinedTags") Map<String, String> userDefinedTags,
        @JsonProperty("defaultTags") Map<String, String> defaultTags) {
        this.defaultTags = defaultTags;
        this.userDefinedTags = userDefinedTags;
    }

    public Map<String, String> getUserDefinedTags() {
        return userDefinedTags;
    }

    public Map<String, String> getDefaultTags() {
        return defaultTags;
    }

    @Override
    public String toString() {
        return "StackTags{"
                + "userDefinedTags=" + userDefinedTags
                + ", defaultTags=" + defaultTags
                + '}';
    }

    static EnvironmentTags fromJson(Json tags) {
        if (tags != null && tags.getValue() != null) {
            return JsonUtil.readValueOpt(tags.getValue(), EnvironmentTags.class)
                    .orElse(new EnvironmentTags(new HashMap<>(), new HashMap<>()));
        }
        return new EnvironmentTags(new HashMap<>(), new HashMap<>());
    }

}
