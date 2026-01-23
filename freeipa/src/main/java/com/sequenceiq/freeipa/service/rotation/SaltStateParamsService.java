package com.sequenceiq.freeipa.service.rotation;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateRetryParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.RetryPredicates;
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

    @Inject
    private RetryPredicates retryPredicates;

    public OrchestratorStateParams createStateParams(Stack stack, String saltState, boolean onlyOnPrimary, int maxRetry, int maxRetryOnError) {
        return createStateParams(stack, saltState, onlyOnPrimary, maxRetry, maxRetryOnError, -1, retryPredicates.retryTransientErrors());
    }

    public OrchestratorStateParams createStateParams(Stack stack, String saltState, boolean onlyOnPrimary, int maxRetry, int maxRetryOnError,
            Predicate<Exception> retryPredicate) {
        return createStateParams(stack, saltState, onlyOnPrimary, maxRetry, maxRetryOnError, -1, retryPredicate);
    }

    public OrchestratorStateParams createStateParams(Stack stack, String saltState, boolean onlyOnPrimary, int maxRetry, int maxRetryOnError,
            int sleepTime, Predicate<Exception> retryPredicate) {
        Set<Node> nodes = freeIpaNodeUtilService.mapInstancesToNodes(stack.getNotDeletedInstanceMetaDataSet());
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        if (onlyOnPrimary) {
            nodes = nodes.stream()
                    .filter(node -> node.getHostname().equals(primaryGatewayConfig.getHostname()))
                    .collect(Collectors.toSet());
        }
        OrchestratorStateRetryParams orchestratorStateRetryParams = getOrchestratorStateRetryParams(maxRetry, maxRetryOnError, sleepTime, retryPredicate);
        return getOrchestratorStateParams(stack, saltState, primaryGatewayConfig, nodes, orchestratorStateRetryParams);
    }

    private OrchestratorStateParams getOrchestratorStateParams(Stack stack, String saltState,
            GatewayConfig primaryGatewayConfig, Set<Node> targetNodes, OrchestratorStateRetryParams retryParams) {
        OrchestratorStateParams stateParams = new OrchestratorStateParams();
        stateParams.setState(saltState);
        stateParams.setPrimaryGatewayConfig(primaryGatewayConfig);
        stateParams.setTargetHostNames(targetNodes.stream().map(Node::getHostname).collect(Collectors.toSet()));
        stateParams.setStateRetryParams(retryParams);
        stateParams.setExitCriteriaModel(exitCriteriaProvider.get(stack));
        return stateParams;
    }

    private OrchestratorStateRetryParams getOrchestratorStateRetryParams(int maxRetry, int maxRetryOnError, int sleepTime,
            Predicate<Exception> retryPredicate) {
        OrchestratorStateRetryParams retryParams = new OrchestratorStateRetryParams();
        retryParams.setMaxRetryOnError(maxRetryOnError);
        retryParams.setMaxRetry(maxRetry);
        retryParams.setSleepTime(sleepTime);
        retryParams.setRetryPredicate(retryPredicate);
        return retryParams;
    }
}
