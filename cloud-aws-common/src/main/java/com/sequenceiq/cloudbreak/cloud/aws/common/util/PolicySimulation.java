package com.sequenceiq.cloudbreak.cloud.aws.common.util;

import java.util.Set;

public class PolicySimulation {

    private final Set<String> resources;

    private final Set<String> actions;

    public PolicySimulation(Set<String> resources, Set<String> actions) {
        this.resources = resources;
        this.actions = actions;
    }

    public Set<String> getResources() {
        return resources;
    }

    public Set<String> getActions() {
        return actions;
    }
}
