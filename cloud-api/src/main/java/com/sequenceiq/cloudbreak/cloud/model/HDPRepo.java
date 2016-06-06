package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HDPRepo {

    private Map<String, String> stack;
    private Map<String, String> util;

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
