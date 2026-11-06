package com.sequenceiq.freeipa.flow.freeipa.provision.event.services;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class InstallFreeIpaServicesRequest extends StackEvent {

    private final boolean skipPreServiceDeploymentRecipes;

    public InstallFreeIpaServicesRequest(Long stackId) {
        this(stackId, false);
    }

    @JsonCreator
    public InstallFreeIpaServicesRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("skipPreServiceDeploymentRecipes") boolean skipPreServiceDeploymentRecipes) {
        super(stackId);
        this.skipPreServiceDeploymentRecipes = skipPreServiceDeploymentRecipes;
    }

    public boolean isSkipPreServiceDeploymentRecipes() {
        return skipPreServiceDeploymentRecipes;
    }

    @Override
    public String toString() {
        return "InstallFreeIpaServicesRequest{" +
                "skipPreServiceDeploymentRecipes=" + skipPreServiceDeploymentRecipes +
                "} " + super.toString();
    }
}
