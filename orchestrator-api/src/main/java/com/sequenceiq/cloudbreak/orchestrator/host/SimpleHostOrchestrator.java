package com.sequenceiq.cloudbreak.orchestrator.host;

import com.sequenceiq.cloudbreak.orchestrator.executor.ParallelContainerRunner;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;

public abstract class SimpleHostOrchestrator implements HostOrchestrator {

    private ParallelContainerRunner parallelContainerRunner;
    private ExitCriteria exitCriteria;

    @Override
    public void init(ParallelContainerRunner parallelContainerRunner, ExitCriteria exitCriteria) {
        this.parallelContainerRunner = parallelContainerRunner;
        this.exitCriteria = exitCriteria;
    }

    public ParallelContainerRunner getParallelContainerRunner() {
        return parallelContainerRunner;
    }

    protected ExitCriteria getExitCriteria() {
        return exitCriteria;
    }
}
