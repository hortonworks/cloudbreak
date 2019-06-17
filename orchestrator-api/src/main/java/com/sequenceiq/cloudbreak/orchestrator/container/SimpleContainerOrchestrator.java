package com.sequenceiq.cloudbreak.orchestrator.container;

import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;

public abstract class SimpleContainerOrchestrator implements ContainerOrchestrator {

    private ExitCriteria exitCriteria;

    @Override
    public void init(ExitCriteria exitCriteria) {
        this.exitCriteria = exitCriteria;
    }

    protected ExitCriteria getExitCriteria() {
        return exitCriteria;
    }
}
