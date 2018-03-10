package com.sequenceiq.cloudbreak.cloud.model.catalog;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StackRepoDetails {

    public static final String REPO_ID_TAG = "repoid";

    public static final String MPACK_TAG = "mpack";

    private final Map<String, String> stack;

    private final Map<String, String> util;

    private final Map<String, String> knox;

    @JsonCreator
    public StackRepoDetails(
            @JsonProperty(value = "stack", required = true) Map<String, String> stack,
            @JsonProperty(value = "util", required = true) Map<String, String> util,
            @JsonProperty("knox") Map<String, String> knox) {
        this.stack = stack;
        this.util = util;
        this.knox = (knox == null) ? Collections.emptyMap() : knox;
    }

    public Map<String, String> getStack() {
        return stack;
    }

    public Map<String, String> getUtil() {
        return util;
    }

    public Map<String, String> getKnox() {
        return knox;
    }

    @Override
    public String toString() {
        return "StackRepoDetails{stack='" + stack.get(REPO_ID_TAG) + "'; utils='" + util.get(REPO_ID_TAG) + "'}";
    }
}
