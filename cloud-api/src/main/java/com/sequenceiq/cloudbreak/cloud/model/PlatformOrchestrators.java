package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Collection;
import java.util.Map;

public class PlatformOrchestrators {

    private final Map<Platform, Collection<Orchestrator>> orchestrators;
    private final Map<Platform, Orchestrator> defaults;

    public PlatformOrchestrators(Map<Platform, Collection<Orchestrator>> orchestrators, Map<Platform, Orchestrator> defaults) {
        this.orchestrators = orchestrators;
        this.defaults = defaults;
    }

    public Map<Platform, Collection<Orchestrator>> getOrchestrators() {
        return orchestrators;
    }

    public Map<Platform, Orchestrator> getDefaults() {
        return defaults;
    }

}
