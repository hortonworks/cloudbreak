package com.sequenceiq.cloudbreak.orchestrator.container;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConstraint;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerInfo;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

public interface ContainerOrchestrator extends ContainerOrchestrationBootstrap {

    void validateApiEndpoint(OrchestrationCredential cred) throws CloudbreakOrchestratorException;

    List<ContainerInfo> runContainer(ContainerConfig config, OrchestrationCredential cred, ContainerConstraint constraint, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException;

    void startContainer(List<ContainerInfo> info, OrchestrationCredential cred);

    void stopContainer(List<ContainerInfo> info, OrchestrationCredential cred);

    void deleteContainer(List<ContainerInfo> containerInfos, OrchestrationCredential cred) throws CloudbreakOrchestratorException;

    List<String> getMissingNodes(GatewayConfig gatewayConfig, Set<Node> nodes);

    List<String> getAvailableNodes(GatewayConfig gatewayConfig, Set<Node> nodes);

    String ambariServerContainer(Optional<String> name);

    String ambariClientContainer(Optional<String> name);

    String ambariDbContainer(Optional<String> name);

}
