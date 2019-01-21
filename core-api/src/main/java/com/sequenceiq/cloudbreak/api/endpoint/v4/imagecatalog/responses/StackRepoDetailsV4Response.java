package com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StackRepoDetailsV4Response implements JsonEntity {

    @JsonProperty("stack")
    private Map<String, String> stack = new HashMap<>();

    @JsonProperty("util")
    private Map<String, String> util = new HashMap<>();

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
}
