package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Collection;

import com.sequenceiq.cloudbreak.cloud.model.generic.CloudTypes;

public class PlatformOrchestrator extends CloudTypes<Orchestrator> {

    public PlatformOrchestrator(Collection<Orchestrator> types, Orchestrator defaultType) {
        super(types, defaultType);
    }
}
