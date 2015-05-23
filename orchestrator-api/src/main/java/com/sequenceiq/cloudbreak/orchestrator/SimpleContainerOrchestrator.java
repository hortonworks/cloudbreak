package com.sequenceiq.cloudbreak.orchestrator;

public abstract class SimpleContainerOrchestrator implements ContainerOrchestrator {

    private ParallelContainerRunner parallelContainerRunner;

    @Override
    public void init(ParallelContainerRunner parallelContainerRunner) {
        this.parallelContainerRunner = parallelContainerRunner;
    }

    protected ParallelContainerRunner getParallelContainerRunner() {
        return parallelContainerRunner;
    }
}
