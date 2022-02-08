package com.sequenceiq.cloudbreak.service.recovery;

import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.GrainOperation;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorGrainRunnerParams;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Service
public class RdsRecoverySetupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdsRecoverySetupService.class);

    private  static final String RECOVER = "recover";

    private static final String ROLES = "roles";

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private StackService stackService;

    public void addRecoverRole(Long stackId) throws CloudbreakOrchestratorFailedException {
        modifyRecoverRole(stackId, GrainOperation.ADD);
    }

    public void removeRecoverRole(Long stackId) throws CloudbreakOrchestratorFailedException {
        modifyRecoverRole(stackId, GrainOperation.REMOVE);
    }

    private void modifyRecoverRole(Long stackId, GrainOperation operation) throws CloudbreakOrchestratorFailedException {
        OrchestratorGrainRunnerParams stateParams = createRecoverGrainRunnerParams(stackService.getByIdWithListsInTransaction(stackId), operation);
        LOGGER.debug("{} 'recover' role with params {}", operation.name().toLowerCase(), stateParams);
        hostOrchestrator.runOrchestratorGrainRunner(stateParams);
    }

    private OrchestratorGrainRunnerParams createRecoverGrainRunnerParams(Stack stack, GrainOperation operation) {
        Set<Node> nodes = stackUtil.collectReachableNodes(stack);
        return createOrchestratorGrainRunnerParams(stack, stack.getCluster(), nodes, operation);
    }

    private OrchestratorGrainRunnerParams createOrchestratorGrainRunnerParams(Stack stack, Cluster cluster, Set<Node> nodes, GrainOperation grainOperation) {
        OrchestratorGrainRunnerParams grainRunnerParams = new OrchestratorGrainRunnerParams();
        InstanceMetaData gatewayInstance = stack.getPrimaryGatewayInstance();
        grainRunnerParams.setPrimaryGatewayConfig(gatewayConfigService.getGatewayConfig(stack, gatewayInstance, stack.getCluster().hasGateway()));
        Set<String> targetHostNames = gatewayInstance.getDiscoveryFQDN() != null ? Set.of(gatewayInstance.getDiscoveryFQDN()) : Set.of();
        grainRunnerParams.setTargetHostNames(targetHostNames);
        grainRunnerParams.setAllNodes(nodes);
        grainRunnerParams.setExitCriteriaModel(ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel(stack.getId(), cluster.getId()));
        grainRunnerParams.setKey(ROLES);
        grainRunnerParams.setValue(RECOVER);
        grainRunnerParams.setGrainOperation(grainOperation);
        return grainRunnerParams;
    }

}
