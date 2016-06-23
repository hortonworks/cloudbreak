package com.sequenceiq.cloudbreak.orchestrator.container

import com.sequenceiq.cloudbreak.orchestrator.executor.ParallelOrchestratorComponentRunner
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria

abstract class SimpleContainerOrchestrator : ContainerOrchestrator {

    var parallelOrchestratorComponentRunner: ParallelOrchestratorComponentRunner? = null
        private set
    protected var exitCriteria: ExitCriteria? = null
        private set

    override fun init(parallelOrchestratorComponentRunner: ParallelOrchestratorComponentRunner, exitCriteria: ExitCriteria) {
        this.parallelOrchestratorComponentRunner = parallelOrchestratorComponentRunner
        this.exitCriteria = exitCriteria
    }
}
