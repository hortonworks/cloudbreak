package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class StartAmbariServicesRequest extends StackEvent {

    private final boolean defaultClusterManagerAuth;

    private final boolean runPreServiceDeploymentRecipe;

    @JsonCreator
    public StartAmbariServicesRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("defaultClusterManagerAuth") boolean defaultClusterManagerAuth,
            @JsonProperty("runPreServiceDeploymentRecipe") boolean runPreServiceDeploymentRecipe) {
        super(stackId);
        this.defaultClusterManagerAuth = defaultClusterManagerAuth;
        this.runPreServiceDeploymentRecipe = runPreServiceDeploymentRecipe;
    }

    public boolean isDefaultClusterManagerAuth() {
        return defaultClusterManagerAuth;
    }

    public boolean isRunPreServiceDeploymentRecipe() {
        return runPreServiceDeploymentRecipe;
    }
}
