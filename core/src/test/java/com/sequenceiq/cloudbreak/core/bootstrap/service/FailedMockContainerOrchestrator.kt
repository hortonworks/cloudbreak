package com.sequenceiq.cloudbreak.core.bootstrap.service

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConfig
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConstraint
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerInfo
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel

class FailedMockContainerOrchestrator : MockContainerOrchestrator() {
    @Throws(CloudbreakOrchestratorException::class)
    override fun runContainer(config: ContainerConfig, cred: OrchestrationCredential, constraint: ContainerConstraint,
                              exitCriteriaModel: ExitCriteriaModel): List<ContainerInfo>? {
        throw CloudbreakOrchestratorFailedException("failed")
    }
}
