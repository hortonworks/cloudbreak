package com.sequenceiq.cloudbreak.core.bootstrap.service;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.ContainerOrchestratorCluster;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConfig;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.orchestrator.model.LogVolumePath;

public class FailedMockContainerOrchestrator extends MockContainerOrchestrator {
    @Override
    public void startAmbariAgents(ContainerOrchestratorCluster cluster, ContainerConfig config, String platform, LogVolumePath logVolumePath,
            ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorCancelledException, CloudbreakOrchestratorFailedException {
        throw new CloudbreakOrchestratorFailedException("failed");
    }
}
