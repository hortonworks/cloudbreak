package com.sequenceiq.cloudbreak.cloud.model.component;

import java.io.Serializable;
import java.util.Map;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClouderaManagerDefaultStackRepoDetails implements Serializable {

    private Map<String, String> stack;

    private String cdhVersion;

    public Map<String, String> getStack() {
        return stack;
    }

    public void setStack(Map<String, String> stack) {
        this.stack = stack;
    }

    public String getCdhVersion() {
        return cdhVersion;
    }

    public void setCdhVersion(String cdhVersion) {
        this.cdhVersion = cdhVersion;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ClouderaManagerDefaultStackRepoDetails.class.getSimpleName() + '[', "]")
                .add("stack=" + stack)
                .add("cdhVersion='" + cdhVersion + '\'')
                .toString();
    }
}
