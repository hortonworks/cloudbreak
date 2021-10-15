package com.sequenceiq.cloudbreak.cloud.model.catalog;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StackRepoDetails {
    public static final String REPO_ID_TAG = "repoid";

    private static final String STACK = "stack";

    private static final String UTIL = "util";

    private final Map<String, String> stack;

    private final Map<String, String> util;

    @JsonCreator
    public StackRepoDetails(
            @JsonProperty(value = STACK, required = true) Map<String, String> stack,
            @JsonProperty(value = UTIL) Map<String, String> util) {
        this.stack = stack;
        this.util = util;
    }

    @JsonProperty(STACK)
    public Map<String, String> getStack() {
        return stack;
    }

    @JsonProperty(UTIL)
    public Map<String, String> getUtil() {
        return util;
    }

    @Override
    public String toString() {
        return "StackRepoDetails{stack='" + stack.get(REPO_ID_TAG) + "'; utils='" + util.get(REPO_ID_TAG) + "'}";
    }
}
