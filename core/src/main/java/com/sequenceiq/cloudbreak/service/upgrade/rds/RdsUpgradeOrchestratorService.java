package com.sequenceiq.cloudbreak.service.upgrade.rds;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Service
public class RdsUpgradeOrchestratorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdsUpgradeOrchestratorService.class);

    private static final String BACKUP_STATE = "postgresql/upgrade/backup";

    private static final String RESTORE_STATE = "postgresql/upgrade/restore";

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private HostOrchestrator hostOrchestrator;

    public void backupRdsData(Long stackId) throws CloudbreakOrchestratorException {
        OrchestratorStateParams stateParams = createStateParams(stackId, BACKUP_STATE);
        LOGGER.debug("Calling backupRdsData with state params '{}'", stateParams);
        hostOrchestrator.runOrchestratorState(stateParams);
    }

    public void restoreRdsData(Long stackId) throws CloudbreakOrchestratorException {
        OrchestratorStateParams stateParams = createStateParams(stackId, RESTORE_STATE);
        LOGGER.debug("Calling restoreRdsData with state params '{}'", stateParams);
        hostOrchestrator.runOrchestratorState(stateParams);
    }

    private OrchestratorStateParams createStateParams(Long stackId, String saltState) {
        StackDto stack = stackDtoService.getById(stackId);
        Set<Node> gatewayNodes = stackUtil.collectGatewayNodes(stack);

        OrchestratorStateParams stateParams = new OrchestratorStateParams();
        stateParams.setState(saltState);
        stateParams.setPrimaryGatewayConfig(gatewayConfigService.getPrimaryGatewayConfig(stack));
        stateParams.setTargetHostNames(gatewayNodes.stream().map(Node::getHostname).collect(Collectors.toSet()));
        stateParams.setExitCriteriaModel(new ClusterDeletionBasedExitCriteriaModel(stack.getId(), stack.getCluster().getId()));
        return stateParams;
    }
}
