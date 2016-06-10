package com.sequenceiq.cloudbreak.orchestrator.model;

public enum RecipeExecutionPhase {
    PRE("pre"), POST("post");

    private String value;

    RecipeExecutionPhase(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

}
