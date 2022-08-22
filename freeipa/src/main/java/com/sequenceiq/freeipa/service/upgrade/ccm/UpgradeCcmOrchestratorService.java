package com.sequenceiq.freeipa.service.upgrade.ccm;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.orchestrator.StackBasedExitCriteriaModel;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaNodeUtilService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class UpgradeCcmOrchestratorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeCcmOrchestratorService.class);

    private static final String UPGRADE_CCM_STATE = "upgradeccm";

    private static final String NGINX_STATE = "nginx";

    private static final String DISABLE_MINA_STATE = "upgradeccm/disable-ccmv1";

    @Inject
    private StackService stackService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private FreeIpaNodeUtilService freeIpaNodeUtilService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    public void applyUpgradeState(Long stackId) throws CloudbreakOrchestratorException {
        OrchestratorStateParams stateParams = createStateParams(stackId, UPGRADE_CCM_STATE);
        LOGGER.debug("Calling applyUpgradeState with state params '{}'", stateParams);
        hostOrchestrator.runOrchestratorState(stateParams);
    }

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

    private OrchestratorStateParams createStateParams(Long stackId, String saltState) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Set<InstanceMetaData> instanceMetaDatas = stack.getNotDeletedInstanceMetaDataSet();
        Set<Node> allNodes = freeIpaNodeUtilService.mapInstancesToNodes(instanceMetaDatas);

        OrchestratorStateParams stateParams = new OrchestratorStateParams();
        stateParams.setState(saltState);
        stateParams.setPrimaryGatewayConfig(gatewayConfigService.getPrimaryGatewayConfig(stack));
        stateParams.setTargetHostNames(allNodes.stream().map(Node::getHostname).collect(Collectors.toSet()));
        stateParams.setAllNodes(allNodes);
        stateParams.setExitCriteriaModel(new StackBasedExitCriteriaModel(stack.getId()));
        return stateParams;
    }
}
