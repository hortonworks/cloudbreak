package com.sequenceiq.cloudbreak.orchestrator;

public abstract class SimpleContainerOrchestrator implements ContainerOrchestrator {

    private ParallelContainerRunner parallelContainerRunner;
    private ExitCriteria exitCriteria;

    @Override
    public void init(ParallelContainerRunner parallelContainerRunner, ExitCriteria exitCriteria) {
        this.parallelContainerRunner = parallelContainerRunner;
        this.exitCriteria = exitCriteria;
    }

    protected ParallelContainerRunner getParallelContainerRunner() {
        return parallelContainerRunner;
    }

    protected ExitCriteria getExitCriteria() {
        return exitCriteria;
    }
}
