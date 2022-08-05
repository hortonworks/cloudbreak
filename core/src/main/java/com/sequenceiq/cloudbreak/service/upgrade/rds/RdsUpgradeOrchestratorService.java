package com.sequenceiq.cloudbreak.service.upgrade.rds;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateRetryParams;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Service
public class RdsUpgradeOrchestratorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdsUpgradeOrchestratorService.class);

    private static final String BACKUP_STATE = "postgresql/upgrade/backup";

    private static final String RESTORE_STATE = "postgresql/upgrade/restore";

    private static final String UPGRADE_EMBEDDED_DATABASE = "postgresql/upgrade/embedded";

    private static final String PREPARE_UPGRADE_EMBEDDED_DATABASE = "postgresql/upgrade/prepare-embedded";

    private static final int DB_SPACE_MULTIPLIER = 3;

    private static final int KB_TO_MB = 1024;

    private static final int MAX_RETRY_ON_ERROR = 5;

    private static final int MAX_RETRY = 10;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private UpgradeEmbeddedDBStateParamsProvider upgradeEmbeddedDBStateParamsProvider;

    @Inject
    private UpgradeEmbeddedDBPreparationStateParamsProvider upgradeEmbeddedDBPreparationStateParamsProvider;

    @Inject
    private BackupRestoreEmbeddedDBStateParamsProvider backupRestoreEmbeddedDBStateParamsProvider;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private HostOrchestrator hostOrchestrator;

    public void backupRdsData(Long stackId) throws CloudbreakOrchestratorException {
        OrchestratorStateParams stateParams = createStateParams(stackId, BACKUP_STATE);
        stateParams.setStateParams(backupRestoreEmbeddedDBStateParamsProvider.createParamsForBackupRestore());
        LOGGER.debug("Calling backupRdsData with state params '{}'", stateParams);
        hostOrchestrator.runOrchestratorState(stateParams);
    }

    public void restoreRdsData(Long stackId) throws CloudbreakOrchestratorException {
        OrchestratorStateParams stateParams = createStateParams(stackId, RESTORE_STATE);
        stateParams.setStateParams(backupRestoreEmbeddedDBStateParamsProvider.createParamsForBackupRestore());
        LOGGER.debug("Calling restoreRdsData with state params '{}'", stateParams);
        hostOrchestrator.runOrchestratorState(stateParams);
    }

    public void upgradeEmbeddedDatabase(StackDto stackDto) throws CloudbreakOrchestratorException {
        OrchestratorStateParams stateParams = createStateParams(stackDto, UPGRADE_EMBEDDED_DATABASE, false);
        stateParams.setStateParams(upgradeEmbeddedDBStateParamsProvider.createParamsForEmbeddedDBUpgrade(stackDto));
        LOGGER.debug("Calling upgradeEmbeddedDatabase with state params '{}'", stateParams);
        hostOrchestrator.runOrchestratorState(stateParams);
    }

    public void validateDbDirectorySpace(Long stackId) throws CloudbreakOrchestratorException {
        OrchestratorStateParams stateParams = createStateParams(stackId, PREPARE_UPGRADE_EMBEDDED_DATABASE, true);
        Map<String, String> dbVolumeSizeMap = hostOrchestrator.runCommandOnHosts(List.of(stateParams.getPrimaryGatewayConfig()),
                stateParams.getTargetHostNames(), "df -k /dbfs | tail -1 | awk '{print $4}'");
        Map<String, String> dataDirSizeMap = hostOrchestrator.runCommandOnHosts(List.of(stateParams.getPrimaryGatewayConfig()),
                stateParams.getTargetHostNames(), "du -sk /dbfs/pgsql | awk '{print $1}'");
        Optional<Integer> dataDirSize = Optional.ofNullable(dataDirSizeMap.get(stateParams.getPrimaryGatewayConfig().getHostname())).map(Integer::valueOf);
        Optional<Integer> dbVolumeSize = Optional.ofNullable(dbVolumeSizeMap.get(stateParams.getPrimaryGatewayConfig().getHostname())).map(Integer::valueOf);
        if (dataDirSize.isPresent() && dbVolumeSize.isPresent()) {
            if (dataDirSize.get() * DB_SPACE_MULTIPLIER > dbVolumeSize.get()) {
                String msg = "Not enough space on attached db volume for postgres upgrade. Needed "
                        + (dataDirSize.get() * DB_SPACE_MULTIPLIER / KB_TO_MB) + "MB, available: "
                        + (dbVolumeSize.get() / KB_TO_MB) + "MB";
                LOGGER.warn(msg);
                throw new CloudbreakOrchestratorFailedException(msg);
            }
        } else {
            String msg = "Space validation on attached db volume failed.";
            LOGGER.warn(msg);
            throw new CloudbreakOrchestratorFailedException(msg);
        }
    }

    public void prepareUpgradeEmbeddedDatabase(Long stackId) throws CloudbreakOrchestratorException {
        StackDto stackDto = stackDtoService.getById(stackId);
        OrchestratorStateParams stateParams = createStateParams(stackDto, PREPARE_UPGRADE_EMBEDDED_DATABASE, false);
        stateParams.setStateParams(upgradeEmbeddedDBPreparationStateParamsProvider.createParamsForEmbeddedDBUpgradePreparation(stackDto));
        LOGGER.debug("Calling prepareUpgradeOfEmbeddedDatabase with state params '{}'", stateParams);
        hostOrchestrator.runOrchestratorState(stateParams);
    }

    private OrchestratorStateParams createStateParams(Long stackId, String saltState) {
        return createStateParams(stackId, saltState, false);
    }

    private OrchestratorStateParams createStateParams(Long stackId, String saltState, boolean onlyOnPrimary) {
        StackDto stack = stackDtoService.getById(stackId);
        return createStateParams(stack, saltState, onlyOnPrimary);
    }

    private OrchestratorStateParams createStateParams(StackDto stack, String saltState, boolean onlyOnPrimary) {
        Set<Node> gatewayNodes = stackUtil.collectGatewayNodes(stack);
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        if (onlyOnPrimary) {
            gatewayNodes = gatewayNodes.stream()
                    .filter(node -> node.getHostname().equals(primaryGatewayConfig.getHostname()))
                    .collect(Collectors.toSet());
        }
        OrchestratorStateParams stateParams = new OrchestratorStateParams();
        stateParams.setState(saltState);
        stateParams.setPrimaryGatewayConfig(primaryGatewayConfig);
        stateParams.setTargetHostNames(gatewayNodes.stream().map(Node::getHostname).collect(Collectors.toSet()));
        OrchestratorStateRetryParams retryParams = new OrchestratorStateRetryParams();
        retryParams.setMaxRetryOnError(MAX_RETRY_ON_ERROR);
        retryParams.setMaxRetry(MAX_RETRY);
        stateParams.setStateRetryParams(retryParams);
        stateParams.setExitCriteriaModel(new ClusterDeletionBasedExitCriteriaModel(stack.getId(), stack.getCluster().getId()));
        return stateParams;
    }
}
