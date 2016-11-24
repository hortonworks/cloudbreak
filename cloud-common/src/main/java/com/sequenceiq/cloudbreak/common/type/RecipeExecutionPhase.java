package com.sequenceiq.cloudbreak.common.type;

public enum RecipeExecutionPhase {
    PRE("pre"), POST("post");

    private String value;
    private String url;

    RecipeExecutionPhase(String value) {
        this.value = value;
        this.url = value + "-url";
    }

    public String value() {
        return value;
    }

    public String url() {
        return url;
    }
}
