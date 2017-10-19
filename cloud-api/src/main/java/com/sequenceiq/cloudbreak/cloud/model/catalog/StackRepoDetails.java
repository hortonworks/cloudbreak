package com.sequenceiq.cloudbreak.cloud.model.catalog;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StackRepoDetails {

    public static final String REPO_ID_TAG = "repoid";

    public static final String MPACK_TAG = "mpack";

    @JsonProperty("stack")
    private Map<String, String> stack;

    @JsonProperty("util")
    private Map<String, String> util;

    @JsonProperty("knox")
    private Map<String, String> knox;

    public Map<String, String> getStack() {
        return stack;
    }

    public void setStack(Map<String, String> stack) {
        this.stack = stack;
    }

    public Map<String, String> getUtil() {
        return util;
    }

    public void setUtil(Map<String, String> util) {
        this.util = util;
    }

    public Map<String, String> getKnox() {
        return knox;
    }

    public void setKnox(Map<String, String> knox) {
        this.knox = knox;
    }

    @Override
    public String toString() {
        return "StackRepoDetails{stack='" + stack.get(REPO_ID_TAG) + "'; utils='" + util.get(REPO_ID_TAG) + "'}";
    }
}
