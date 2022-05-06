package com.sequenceiq.cloudbreak.service.upgrade.ccm;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Service
public class UpgradeCcmOrchestratorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeCcmOrchestratorService.class);

    private static final String NGINX_STATE = "nginx";

    private static final String DISABLE_MINA_STATE = "upgradeccm/disable-ccmv1";

    private static final String DISABLE_INVERTING_PROXY_AGENT_STATE = "upgradeccm/disable-ccmv2";

    @Inject
    private StackService stackService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private HostOrchestrator hostOrchestrator;

    public void reconfigureNginx(Long stackId) throws CloudbreakOrchestratorException {
        OrchestratorStateParams stateParams = createStateParams(stackId, NGINX_STATE);
        LOGGER.debug("Calling reconfigureNginx with state params '{}'", stateParams);
        hostOrchestrator.runOrchestratorState(stateParams);
    }

    public void disableMina(Long stackId) throws CloudbreakOrchestratorException {
        OrchestratorStateParams stateParams = createStateParams(stackId, DISABLE_MINA_STATE);
        LOGGER.debug("Calling disableMina with state params '{}'", stateParams);
        hostOrchestrator.runOrchestratorState(stateParams);
    }

    public void disableInvertingProxyAgent(Long stackId) throws CloudbreakOrchestratorException {
        OrchestratorStateParams stateParams = createStateParams(stackId, DISABLE_INVERTING_PROXY_AGENT_STATE);
        LOGGER.debug("Calling disableInvertingProxyAgent with state params '{}'", stateParams);
        hostOrchestrator.runOrchestratorState(stateParams);
    }

    private OrchestratorStateParams createStateParams(Long stackId, String saltState) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Set<Node> gatewayNodes = stackUtil.collectGatewayNodes(stack);

        OrchestratorStateParams stateParams = new OrchestratorStateParams();
        stateParams.setState(saltState);
        stateParams.setPrimaryGatewayConfig(gatewayConfigService.getPrimaryGatewayConfig(stack));
        stateParams.setTargetHostNames(gatewayNodes.stream().map(Node::getHostname).collect(Collectors.toSet()));
        stateParams.setAllNodes(gatewayNodes);
        stateParams.setExitCriteriaModel(new ClusterDeletionBasedExitCriteriaModel(stack.getId(), stack.getCluster().getId()));
        return stateParams;
    }
}
