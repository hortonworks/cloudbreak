package com.sequenceiq.cloudbreak.core.bootstrap.service;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.ContainerOrchestratorCluster;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.orchestrator.model.LogVolumePath;

public class CancelledMockContainerOrchestrator extends MockContainerOrchestrator {
    @Override
    public void startAmbariAgents(ContainerOrchestratorCluster cluster, String imageName, int count, String platform,
            LogVolumePath logVolumePath, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorCancelledException, CloudbreakOrchestratorFailedException {
        throw new CloudbreakOrchestratorCancelledException("cancelled");
    }
}
