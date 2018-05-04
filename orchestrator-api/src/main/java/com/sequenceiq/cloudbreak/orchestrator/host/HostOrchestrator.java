package com.sequenceiq.cloudbreak.orchestrator.host;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.executor.ParallelOrchestratorComponentRunner;
import com.sequenceiq.cloudbreak.orchestrator.model.BootstrapParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarConfig;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

public interface HostOrchestrator extends HostRecipeExecutor {

    String name();

    void init(ParallelOrchestratorComponentRunner parallelOrchestratorComponentRunner, ExitCriteria exitCriteria);

    void bootstrap(List<GatewayConfig> allGatewayConfigs, Set<Node> targets, BootstrapParams params, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException;

    void bootstrapNewNodes(List<GatewayConfig> allGatewayConfigs, Set<Node> nodes, Set<Node> allNodes, BootstrapParams params,
            ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException;

    boolean isBootstrapApiAvailable(GatewayConfig gatewayConfig);

    void runService(List<GatewayConfig> allGatewayConfigs, Set<Node> allNodes, SaltPillarConfig pillarConfig, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException;

    void resetAmbari(GatewayConfig gatewayConfig, Set<String> target, Set<Node> allNodes, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException;

    void upgradeAmbari(GatewayConfig gatewayConfig, Set<String> target, Set<Node> allNodes, SaltPillarConfig pillarConfig, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException;

    List<String> getMissingNodes(GatewayConfig gatewayConfig, Set<Node> nodes);

    List<String> getAvailableNodes(GatewayConfig gatewayConfig, Set<Node> nodes);

    void tearDown(List<GatewayConfig> allGatewayConfigs, Map<String, String> privateIPsByFQDN) throws CloudbreakOrchestratorException;

    Map<String, String> getMembers(GatewayConfig gatewayConfig, List<String> privateIps) throws CloudbreakOrchestratorException;

    void changePrimaryGateway(GatewayConfig formerGateway, GatewayConfig newPrimaryGateway, List<GatewayConfig> allGatewayConfigs, Set<Node> allNodes,
            ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException;
}
