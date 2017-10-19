package com.sequenceiq.cloudbreak.api.model.imagecatalog;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StackRepoDetailsJson implements JsonEntity {

    @JsonProperty("stack")
    private Map<String, String> stack = new HashMap<>();

    @JsonProperty("util")
    private Map<String, String> util = new HashMap<>();

    @JsonProperty("knox")
    private Map<String, String> knox = new HashMap<>();

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
}
