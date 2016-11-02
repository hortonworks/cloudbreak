package com.sequenceiq.cloudbreak.orchestrator.container;

import com.sequenceiq.cloudbreak.orchestrator.executor.ParallelOrchestratorComponentRunner;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;

public abstract class SimpleContainerOrchestrator implements ContainerOrchestrator {

    private ParallelOrchestratorComponentRunner parallelOrchestratorComponentRunner;

    private ExitCriteria exitCriteria;

    @Override
    public void init(ParallelOrchestratorComponentRunner parallelOrchestratorComponentRunner, ExitCriteria exitCriteria) {
        this.parallelOrchestratorComponentRunner = parallelOrchestratorComponentRunner;
        this.exitCriteria = exitCriteria;
    }

    public ParallelOrchestratorComponentRunner getParallelOrchestratorComponentRunner() {
        return parallelOrchestratorComponentRunner;
    }

    protected ExitCriteria getExitCriteria() {
        return exitCriteria;
    }
}
