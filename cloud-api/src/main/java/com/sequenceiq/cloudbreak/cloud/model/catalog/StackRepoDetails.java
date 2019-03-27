package com.sequenceiq.cloudbreak.cloud.model.catalog;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StackRepoDetails {
    public static final String REPO_ID_TAG = "repoid";

    private final Map<String, String> stack;

    private final Map<String, String> util;

    @JsonCreator
    public StackRepoDetails(
            @JsonProperty(value = "stack", required = true) Map<String, String> stack,
            @JsonProperty(value = "util") Map<String, String> util) {
        this.stack = stack;
        this.util = util;
    }

    public Map<String, String> getStack() {
        return stack;
    }

    public Map<String, String> getUtil() {
        return util;
    }

    @Override
    public String toString() {
        return "StackRepoDetails{stack='" + stack.get(REPO_ID_TAG) + "'; utils='" + util.get(REPO_ID_TAG) + "'}";
    }
}
