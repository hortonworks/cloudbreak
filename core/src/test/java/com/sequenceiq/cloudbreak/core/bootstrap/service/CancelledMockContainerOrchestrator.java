package com.sequenceiq.cloudbreak.core.bootstrap.service;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConstraint;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerInfo;
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

import java.util.List;

public class CancelledMockContainerOrchestrator extends MockContainerOrchestrator {
    @Override
    public List<ContainerInfo> runContainer(ContainerConfig config, OrchestrationCredential cred, ContainerConstraint constraint,
                                            ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException {
        throw new CloudbreakOrchestratorCancelledException("cancelled");
    }
}
