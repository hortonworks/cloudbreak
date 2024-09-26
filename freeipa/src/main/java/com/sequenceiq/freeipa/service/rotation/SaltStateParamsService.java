package com.sequenceiq.freeipa.service.rotation;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateRetryParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaNodeUtilService;

@Service
public class SaltStateParamsService {

    @Inject
    private FreeIpaNodeUtilService freeIpaNodeUtilService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private ExitCriteriaProvider exitCriteriaProvider;

    public OrchestratorStateParams createStateParams(Stack stack, String saltState, boolean onlyOnPrimary, int maxRetry, int maxRetryOnError) {
        return createStateParams(stack, saltState, onlyOnPrimary, maxRetry, maxRetryOnError, -1);
    }

    public OrchestratorStateParams createStateParams(Stack stack, String saltState, boolean onlyOnPrimary, int maxRetry, int maxRetryOnError,
            int sleepTime) {
        Set<Node> nodes = freeIpaNodeUtilService.mapInstancesToNodes(stack.getNotDeletedInstanceMetaDataSet());
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        if (onlyOnPrimary) {
            nodes = nodes.stream()
                    .filter(node -> node.getHostname().equals(primaryGatewayConfig.getHostname()))
                    .collect(Collectors.toSet());
        }
        return getOrchestratorStateParams(stack, saltState, maxRetry, maxRetryOnError, sleepTime, primaryGatewayConfig, nodes);
    }

    public OrchestratorStateParams createStateParamsForReachableNodes(Stack stack, String saltState, int maxRetry, int maxRetryOnError) {
        Set<Node> nodes = freeIpaNodeUtilService.mapInstancesToNodes(stack.getNotDeletedInstanceMetaDataSet());
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        return getOrchestratorStateParams(stack, saltState, maxRetry, maxRetryOnError, -1, primaryGatewayConfig, nodes);
    }

    private OrchestratorStateParams getOrchestratorStateParams(Stack stack, String saltState, int maxRetry, int maxRetryOnError, int sleepTime,
            GatewayConfig primaryGatewayConfig, Set<Node> targetNodes) {
        OrchestratorStateParams stateParams = new OrchestratorStateParams();
        stateParams.setState(saltState);
        stateParams.setPrimaryGatewayConfig(primaryGatewayConfig);
        stateParams.setTargetHostNames(targetNodes.stream().map(Node::getHostname).collect(Collectors.toSet()));
        OrchestratorStateRetryParams retryParams = new OrchestratorStateRetryParams();
        retryParams.setMaxRetryOnError(maxRetryOnError);
        retryParams.setMaxRetry(maxRetry);
        retryParams.setSleepTime(sleepTime);
        stateParams.setStateRetryParams(retryParams);
        stateParams.setExitCriteriaModel(exitCriteriaProvider.get(stack));
        return stateParams;
    }
}
