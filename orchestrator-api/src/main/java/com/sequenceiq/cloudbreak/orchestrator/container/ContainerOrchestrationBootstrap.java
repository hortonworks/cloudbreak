package com.sequenceiq.cloudbreak.orchestrator.container;

import java.util.Set;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.executor.ParallelContainerRunner;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

public interface ContainerOrchestrationBootstrap {

    String name();

    void init(ParallelContainerRunner parallelContainerRunner, ExitCriteria exitCriteria);

    void bootstrap(GatewayConfig gatewayConfig, ContainerConfig config, Set<Node> nodes, int consulServerCount,
                            ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException;

    void bootstrapNewNodes(GatewayConfig gatewayConfig, ContainerConfig containerConfig, Set<Node> nodes,
                            ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException;

    boolean isBootstrapApiAvailable(GatewayConfig gatewayConfig);

    int getMaxBootstrapNodes();
}
