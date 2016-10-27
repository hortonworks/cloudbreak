package com.sequenceiq.cloudbreak.orchestrator.host;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.executor.ParallelOrchestratorComponentRunner;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarConfig;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

public interface HostOrchestrator extends HostRecipeExecutor {

    String name();

    void init(ParallelOrchestratorComponentRunner parallelOrchestratorComponentRunner, ExitCriteria exitCriteria);

    void bootstrap(GatewayConfig gatewayConfig, Set<Node> targets, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException;

    void bootstrapNewNodes(GatewayConfig gatewayConfig, Set<Node> nodes, ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException;

    boolean isBootstrapApiAvailable(GatewayConfig gatewayConfig);

    int getMaxBootstrapNodes();

    void runService(GatewayConfig gatewayConfig, Set<Node> allNodes, SaltPillarConfig pillarConfig, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException;

    void resetAmbari(GatewayConfig gatewayConfig, Set<String> target, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException;

    List<String> getMissingNodes(GatewayConfig gatewayConfig, Set<Node> nodes);

    List<String> getAvailableNodes(GatewayConfig gatewayConfig, Set<Node> nodes);

    void tearDown(GatewayConfig gatewayConfig, List<String> hostnames) throws CloudbreakOrchestratorException;

    Map<String, String> getMembers(GatewayConfig gatewayConfig, List<String> privateIps) throws CloudbreakOrchestratorException;
}
