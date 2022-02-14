package com.sequenceiq.cloudbreak.service.orchestrator;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.orchestrator.metadata.OrchestratorMetadata;
import com.sequenceiq.cloudbreak.orchestrator.metadata.OrchestratorMetadataProvider;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.repository.OrchestratorRepository;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class OrchestratorService implements OrchestratorMetadataProvider {

    @Inject
    private StackService stackService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private ClusterBootstrapper clusterBootstrapper;

    @Inject
    private OrchestratorRepository repository;

    public Orchestrator save(Orchestrator orchestrator) {
        return repository.save(orchestrator);
    }

    @Override
    public OrchestratorMetadata getOrchestratorMetadata(Long stackId) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
        ClusterDeletionBasedExitCriteriaModel exitModel = new ClusterDeletionBasedExitCriteriaModel(stackId, stack.getCluster().getId());
        return new OrchestratorMetadata(gatewayConfigs, stack.getAllNodes(), exitModel, stack);
    }

    @Override
    public byte[] getStoredStates(Long stackId) {
        Stack stack = stackService.getById(stackId);
        return clusterComponentConfigProvider.getSaltStateComponent(stack.getCluster().getId());
    }

    @Override
    public void storeNewState(Long stackId, byte[] newFullSaltState) {
        Stack stack = stackService.getById(stackId);
        clusterBootstrapper.updateSaltComponent(stack, newFullSaltState);
    }

    @Override
    public List<String> getSaltStateDefinitionBaseFolders() {
        return Arrays.asList("salt-common", "salt");
    }

}
