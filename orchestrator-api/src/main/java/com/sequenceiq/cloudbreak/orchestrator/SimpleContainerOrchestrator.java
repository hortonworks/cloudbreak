package com.sequenceiq.cloudbreak.orchestrator;

public abstract class SimpleContainerOrchestrator implements ContainerOrchestrator {

    private ParallelContainerRunner parallelContainerRunner;

    public SimpleContainerOrchestrator(ParallelContainerRunner parallelContainerRunner) {
        this.parallelContainerRunner = parallelContainerRunner;
    }

    public ParallelContainerRunner getParallelContainerRunner() {
        return parallelContainerRunner;
    }
}
