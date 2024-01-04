package com.sequenceiq.freeipa.service.orchestrator;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorRunParams;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.orchestrator.StackBasedExitCriteriaModel;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaNodeUtilService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class OrchestratorParamsProvider {

    @Inject
    private StackService stackService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private FreeIpaNodeUtilService freeIpaNodeUtilService;

    public OrchestratorStateParams createStateParams(Long stackId, String saltState) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Set<Node> allNodes = freeIpaNodeUtilService.mapInstancesToNodes(stack.getNotDeletedInstanceMetaDataSet());

        OrchestratorStateParams stateParams = getOrchestratorStateParamsWithoutTarget(stack, saltState);
        stateParams.setTargetHostNames(allNodes.stream().map(Node::getHostname).collect(Collectors.toSet()));
        return stateParams;
    }

    public OrchestratorRunParams createRunParams(Long stackId, String command, String errorMessage) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Set<Node> allNodes = freeIpaNodeUtilService.mapInstancesToNodes(stack.getNotDeletedInstanceMetaDataSet());
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getNotDeletedGatewayConfigs(stack);
        return new OrchestratorRunParams(allNodes, gatewayConfigs, command, errorMessage);
    }

    public OrchestratorStateParams createStateParamsForSingleTarget(Stack stack, String hostName, String saltState) {
        OrchestratorStateParams stateParams = getOrchestratorStateParamsWithoutTarget(stack, saltState);
        stateParams.setTargetHostNames(Set.of(hostName));
        return stateParams;
    }

    private OrchestratorStateParams getOrchestratorStateParamsWithoutTarget(Stack stack, String saltState) {
        OrchestratorStateParams stateParams = new OrchestratorStateParams();
        stateParams.setState(saltState);
        stateParams.setPrimaryGatewayConfig(gatewayConfigService.getPrimaryGatewayConfig(stack));
        stateParams.setExitCriteriaModel(new StackBasedExitCriteriaModel(stack.getId()));
        return stateParams;
    }
}
