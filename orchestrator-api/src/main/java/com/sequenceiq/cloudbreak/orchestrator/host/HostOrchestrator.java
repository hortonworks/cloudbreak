package com.sequenceiq.cloudbreak.orchestrator.host;

import java.util.List;
import java.util.Set;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.executor.ParallelOrchestratorComponentRunner;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

public interface HostOrchestrator {

    String name();

    String port();

    void init(ParallelOrchestratorComponentRunner parallelOrchestratorComponentRunner, ExitCriteria exitCriteria);

    void bootstrap(GatewayConfig gatewayConfig, Set<Node> targets, int consulServerCount, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException;

    void bootstrapNewNodes(GatewayConfig gatewayConfig, Set<Node> nodes, ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException;

    boolean isBootstrapApiAvailable(GatewayConfig gatewayConfig);

    int getMaxBootstrapNodes();

    void runService(GatewayConfig gatewayConfig, Set<String> nodes, OrchestrationCredential cred, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException;

    List<String> getMissingNodes(GatewayConfig gatewayConfig, Set<Node> nodes);

    List<String> getAvailableNodes(GatewayConfig gatewayConfig, Set<Node> nodes);

    void tearDown(GatewayConfig gatewayConfig, List<String> hosts) throws CloudbreakOrchestratorException;
}
