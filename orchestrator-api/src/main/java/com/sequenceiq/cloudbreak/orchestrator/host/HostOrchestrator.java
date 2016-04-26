package com.sequenceiq.cloudbreak.orchestrator.host;

import java.util.List;
import java.util.Set;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

public interface HostOrchestrator extends HostOrchestrationBootstrap {

    void validateApiEndpoint(OrchestrationCredential cred) throws CloudbreakOrchestratorException;

    void runService(GatewayConfig gatewayConfig, Set<String> nodes, OrchestrationCredential cred, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException;

    List<String> getMissingNodes(GatewayConfig gatewayConfig, Set<Node> nodes);

    List<String> getAvailableNodes(GatewayConfig gatewayConfig, Set<Node> nodes);
}
