package com.sequenceiq.cloudbreak.service.salt;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateRetryParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Service
public class SaltStateParamsService {

    @Inject
    private StackUtil stackUtil;

    @Inject
    private GatewayConfigService gatewayConfigService;

    public OrchestratorStateParams createStateParams(StackDto stack, String saltState, boolean onlyOnPrimary, int maxRetry, int maxRetryOnError) {
        return createStateParams(stack, saltState, onlyOnPrimary, maxRetry, maxRetryOnError, -1);
    }

    public OrchestratorStateParams createStateParams(StackDto stack, String saltState, boolean onlyOnPrimary, int maxRetry, int maxRetryOnError,
            int sleepTime) {
        Set<Node> gatewayNodes = stackUtil.collectGatewayNodes(stack);
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        if (onlyOnPrimary) {
            gatewayNodes = gatewayNodes.stream()
                    .filter(node -> node.getHostname().equals(primaryGatewayConfig.getHostname()))
                    .collect(Collectors.toSet());
        }
        return getOrchestratorStateParams(stack, saltState, maxRetry, maxRetryOnError, sleepTime, primaryGatewayConfig, gatewayNodes);
    }

    public OrchestratorStateParams createStateParamsForReachableNodes(StackDto stack, String saltState, int maxRetry, int maxRetryOnError) {
        Set<Node> reachableNodes = stackUtil.collectReachableNodes(stack);
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        return getOrchestratorStateParams(stack, saltState, maxRetry, maxRetryOnError, -1, primaryGatewayConfig, reachableNodes);
    }

    private OrchestratorStateParams getOrchestratorStateParams(StackDto stack, String saltState, int maxRetry, int maxRetryOnError, int sleepTime,
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
        stateParams.setExitCriteriaModel(new ClusterDeletionBasedExitCriteriaModel(stack.getId(), stack.getCluster().getId()));
        return stateParams;
    }
}
