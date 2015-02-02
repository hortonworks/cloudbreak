package com.sequenceiq.cloudbreak.service.cluster.flow;

public enum RecipeLifecycleEvent {

    PRE_INSTALL("recipe-pre-install"),
    POST_INSTALL("recipe-post-install");

    private final String name;

    private RecipeLifecycleEvent(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
