package com.sequenceiq.cloudbreak.service.upgrade.rds;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.salt.SaltStateParamsService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.template.VolumeUtils;

@Service
public class RdsUpgradeOrchestratorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdsUpgradeOrchestratorService.class);

    private static final String CHECK_CONNECTION_STATE = "postgresql/upgrade/check-db-connection";

    private static final String BACKUP_STATE = "postgresql/upgrade/backup";

    private static final String RESTORE_STATE = "postgresql/upgrade/restore";

    private static final String PG_INSTALL_STATE = "postgresql/pg-install";

    private static final String PG_ALTERNATIVES_STATE = "postgresql/pg-alternatives";

    private static final String UPGRADE_EMBEDDED_DATABASE = "postgresql/upgrade/embedded";

    private static final String PREPARE_UPGRADE_EMBEDDED_DATABASE = "postgresql/upgrade/prepare-embedded";

    private static final String GET_EXTERNAL_DB_SIZE = "postgresql/upgrade/external-db-size";

    private static final int DB_SPACE_MULTIPLIER = 3;

    private static final int KB_TO_MB = 1024;

    private static final int BYTE_TO_MB = 1024 * 1024;

    private static final int MAX_RETRY_ON_ERROR = 3;

    private static final int LONG_RUNNING_MAX_RETRY_ON_ERROR = 180;

    private static final int MAX_RETRY = 2000;

    private static final int RDS_CONNECT_RETRY = 60;

    private static final int RDS_CONNECT_RETRY_ON_ERROR = 60;

    private static final int RDS_CONNECT_SLEEPTIME = 60000;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private UpgradeEmbeddedDBStateParamsProvider upgradeEmbeddedDBStateParamsProvider;

    @Inject
    private UpgradeEmbeddedDBPreparationStateParamsProvider upgradeEmbeddedDBPreparationStateParamsProvider;

    @Inject
    private BackupRestoreDBStateParamsProvider backupRestoreDBStateParamsProvider;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private UpgradeExternalRdsStateParamsProvider upgradeExternalRdsStateParamsProvider;

    @Inject
    private PostgresConfigService postgresConfigService;

    @Inject
    private SaltStateParamsService saltStateParamsService;

    @Inject
    private EntitlementService entitlementService;

    @Value("${cb.db.env.upgrade.rds.backuprestore.validationratio}")
    private double backupValidationRatio;

    public void checkRdsConnection(StackDto stack) throws CloudbreakOrchestratorException {
        OrchestratorStateParams stateParams = saltStateParamsService.createStateParams(stack, CHECK_CONNECTION_STATE, true, RDS_CONNECT_RETRY,
                RDS_CONNECT_RETRY_ON_ERROR, RDS_CONNECT_SLEEPTIME);
        LOGGER.debug("Calling checkRdsConnection with state params '{}'", stateParams);
        hostOrchestrator.runOrchestratorState(stateParams);
    }

    public void backupRdsData(Long stackId, String backupLocation, String backupInstanceProfile) throws CloudbreakOrchestratorException {
        OrchestratorStateParams stateParams = createStateParams(stackId, BACKUP_STATE, true);
        stateParams.setStateParams(backupRestoreDBStateParamsProvider.createParamsForBackupRestore(backupLocation, backupInstanceProfile));
        LOGGER.debug("Calling backupRdsData with state params '{}'", stateParams);
        hostOrchestrator.runOrchestratorState(stateParams);
    }

    public void restoreRdsData(StackDto stack, String targetVersion) throws CloudbreakOrchestratorException {
        OrchestratorStateParams stateParams = createStateParams(stack, RESTORE_STATE, true);
        stateParams.setStateParams(backupRestoreDBStateParamsProvider.createParamsForBackupRestore(null, null, targetVersion));
        LOGGER.debug("Calling restoreRdsData with state params '{}'", stateParams);
        hostOrchestrator.runOrchestratorState(stateParams);
    }

    public void installPostgresPackages(Long stackId, MajorVersion targetVersion) throws CloudbreakOrchestratorException {
        OrchestratorStateParams stateParams = createStateParams(stackId, PG_INSTALL_STATE, false);
        stateParams.setStateParams(upgradeEmbeddedDBPreparationStateParamsProvider.createParamsWithPostgresVersion());
        LOGGER.debug("Calling installPostgresPackages with state params '{}'", stateParams);
        hostOrchestrator.runOrchestratorState(stateParams);
    }

    public void updatePostgresAlternatives(Long stackId, MajorVersion targetVersion) throws CloudbreakOrchestratorException {
        OrchestratorStateParams stateParams = createStateParams(stackId, PG_ALTERNATIVES_STATE, false);
        stateParams.setStateParams(upgradeEmbeddedDBPreparationStateParamsProvider.createParamsWithPostgresVersion());
        LOGGER.debug("Calling updatePostgresAlternatives with state params '{}'", stateParams);
        hostOrchestrator.runOrchestratorState(stateParams);
    }

    public void upgradeEmbeddedDatabase(StackDto stackDto) throws CloudbreakOrchestratorException {
        OrchestratorStateParams stateParams = createStateParams(stackDto, UPGRADE_EMBEDDED_DATABASE, true);
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
            String msg = "Space validation on attached db volume failed";
            LOGGER.warn(msg);
            throw new CloudbreakOrchestratorFailedException(msg);
        }
    }

    public void prepareUpgradeEmbeddedDatabase(Long stackId) throws CloudbreakOrchestratorException {
        StackDto stackDto = stackDtoService.getById(stackId);
        OrchestratorStateParams stateParams = createStateParams(stackDto, PREPARE_UPGRADE_EMBEDDED_DATABASE, true);
        stateParams.setStateParams(upgradeEmbeddedDBPreparationStateParamsProvider.createParamsForEmbeddedDBUpgradePreparation(stackDto));
        LOGGER.debug("Calling prepareUpgradeOfEmbeddedDatabase with state params '{}'", stateParams);
        hostOrchestrator.runOrchestratorState(stateParams);
    }

    public void determineDbBackupLocation(Long stackId) throws CloudbreakOrchestratorException {
        StackDto stackDto = stackDtoService.getById(stackId);
        cleanUpFormerBackups(stackDto);
        String volumeWithLargestFreeSpace = getVolumeWithLargestFreeSpace(stackDto);
        validateDbBackupSpace(stackDto, volumeWithLargestFreeSpace);
        updateRdsUpgradePillar(stackDto, volumeWithLargestFreeSpace);
    }

    public void validateDbConnection(Long stackId, String serverUrl, String userName) throws CloudbreakOrchestratorException {
        StackDto stack = stackDtoService.getById(stackId);
        OrchestratorStateParams stateParams = createStateParams(stack, CHECK_CONNECTION_STATE, true);
        if (StringUtils.isNotEmpty(serverUrl) && StringUtils.isNotEmpty(userName)) {
            stateParams.setStateParams(upgradeExternalRdsStateParamsProvider.createParamsForRdsCanaryCheck(serverUrl, userName));
            updateCanaryRetryParamsIfLongPollingEnabled(stateParams, stack);
        }
        LOGGER.debug("Calling checkRdsConnection with state params '{}'", stateParams);
        hostOrchestrator.runOrchestratorState(stateParams);
    }

    private void updateCanaryRetryParamsIfLongPollingEnabled(OrchestratorStateParams stateParams, StackDto stack) {
        stateParams.getStateRetryParams().ifPresent(retryParams -> {
            String accountId = Crn.safeFromString(stack.getResourceCrn()).getAccountId();
            if (entitlementService.isFlexibleServerUpgradeLongPollingEnabled(accountId)) {
                retryParams.setMaxRetryOnError(LONG_RUNNING_MAX_RETRY_ON_ERROR);
            }
        });
    }

    public void updateDatabaseEngineVersion(Long stackId) {
        StackDto stackDto = stackDtoService.getById(stackId);
        updateDatabaseEngineVersion(stackDto);
    }

    public void updateDatabaseEngineVersion(StackDto stackDto) {
        LOGGER.debug("Updating the database engine version to {}", stackDto.getDatabase().getExternalDatabaseEngineVersion());
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();
        postgresConfigService.decorateServicePillarWithPostgresIfNeeded(servicePillar, stackDto);
        try {
            hostOrchestrator.saveCustomPillars(new SaltConfig(servicePillar),
                    new ClusterDeletionBasedExitCriteriaModel(stackDto.getId(), stackDto.getCluster().getId()), createStateParams(stackDto, null, true));
        } catch (Exception e) {
            String errorMessage = "Failed to update database engine version in Salt pillar.";
            LOGGER.error(errorMessage, e);
            throw new CloudbreakServiceException(errorMessage, e);
        }
    }

    private void updateRdsUpgradePillar(StackDto stackDto, String rdsBackupLocation) throws CloudbreakOrchestratorException {
        OrchestratorStateParams stateParams = createStateParams(stackDto, GET_EXTERNAL_DB_SIZE, true);
        SaltConfig saltConfig = new SaltConfig(upgradeExternalRdsStateParamsProvider.createParamsForRdsBackupRestore(stackDto, rdsBackupLocation));
        hostOrchestrator.saveCustomPillars(saltConfig, new ClusterDeletionBasedExitCriteriaModel(stackDto.getId(), stackDto.getCluster().getId()), stateParams);
    }

    private void validateDbBackupSpace(StackDto stackDto, String backupLocation) throws CloudbreakOrchestratorException {
        Double freeSpaceMb = getVolumeFreeSpace(stackDto, backupLocation);
        Double databaseSizeMb = determineDatabaseSize(stackDto);
        Double estimatedBackupSizeMb = databaseSizeMb * backupValidationRatio;
        if (freeSpaceMb > estimatedBackupSizeMb) {
            LOGGER.info("{} volume has enough free space ({}MB) for database backup ({}MB).",
                    backupLocation, freeSpaceMb.intValue(), estimatedBackupSizeMb.intValue());
        } else {
            logErrorAndThrow(String.format("%s volume does not have enough free space (%sMB) for database backup (%sMB).",
                    backupLocation, freeSpaceMb.intValue(), estimatedBackupSizeMb.intValue()));
        }
    }

    private Double determineDatabaseSize(StackDto stackDto) throws CloudbreakOrchestratorException {
        OrchestratorStateParams stateParams = createStateParams(stackDto, GET_EXTERNAL_DB_SIZE, true);
        List<Map<String, JsonNode>> result = hostOrchestrator.applyOrchestratorState(stateParams);
        if (CollectionUtils.isEmpty(result) || 1 != result.size()) {
            logErrorAndThrow("Orchestrator engine checking database size did not return any results");
        }

        JsonNode primaryGwResult = result.getFirst().get(stateParams.getPrimaryGatewayConfig().getHostname());
        if (primaryGwResult == null) {
            logErrorAndThrow("Orchestrator engine checking database size did not return any results on the primary gateway");
        }
        Iterable<String> fieldNames = primaryGwResult::fieldNames;
        String fieldName = StreamSupport
                .stream(fieldNames.spliterator(), false)
                .filter(name -> name.startsWith("cmd_|-get_external_db_size_"))
                .findFirst()
                .orElse("");

        JsonNode dbSizeCommandOutput = primaryGwResult.get(fieldName);
        if (dbSizeCommandOutput == null || dbSizeCommandOutput.get("changes") == null) {
            logErrorAndThrow("Orchestrator engine could not run database size checking");
        }
        JsonNode changes = dbSizeCommandOutput.get("changes");
        JsonNode stdErr = changes.get("stderr");
        if (stdErr != null && !stdErr.textValue().isEmpty()) {
            logErrorAndThrow(String.format("Could not determine database size, because of the following error: %s", stdErr.textValue()));
        }
        JsonNode dbSize = changes.get("stdout");
        if (dbSize == null || dbSize.textValue().isEmpty()) {
            logErrorAndThrow("Could not determine database size, because orchestration engine did not have return value");
        }
        return Double.parseDouble(dbSize.textValue()) / BYTE_TO_MB;
    }

    private Double getVolumeFreeSpace(StackDto stackDto, String volume) throws CloudbreakOrchestratorFailedException {
        OrchestratorStateParams stateParams = createStateParams(stackDto, null, true);
        String command = String.format("df -k %s | awk '{print $4}' | tail -n 1", volume);
        Map<String, String> rootVolumeFreeSpaceMap = hostOrchestrator.runCommandOnHosts(List.of(stateParams.getPrimaryGatewayConfig()),
                stateParams.getTargetHostNames(), command);
        Optional<String> rootVolumeFreeSpace = Optional.ofNullable(rootVolumeFreeSpaceMap.get(stateParams.getPrimaryGatewayConfig().getHostname()));
        if (rootVolumeFreeSpace.isPresent() && !rootVolumeFreeSpace.get().isEmpty()) {
            return Double.parseDouble(rootVolumeFreeSpace.get()) / KB_TO_MB;
        } else {
            String msg = "Could not get free space size on root volume from primary gateway";
            LOGGER.warn(msg);
            throw new CloudbreakOrchestratorFailedException(msg);
        }
    }

    private String getVolumeWithLargestFreeSpace(StackDto stackDto) throws CloudbreakOrchestratorFailedException {
        OrchestratorStateParams stateParams = createStateParams(stackDto, null, true);
        String command = String.format("df | grep %s | awk '{print $4\" \"$6}' | sort -nr | head -n 1 | awk '{print $2}'", VolumeUtils.VOLUME_PREFIX);
        Map<String, String> volumeMap = hostOrchestrator.runCommandOnHosts(List.of(stateParams.getPrimaryGatewayConfig()),
                stateParams.getTargetHostNames(), command);
        String volumeWithLargestFreeSpace = volumeMap.get(stateParams.getPrimaryGatewayConfig().getHostname());
        String result = StringUtils.isNotEmpty(volumeWithLargestFreeSpace) ? volumeWithLargestFreeSpace : "/var";
        LOGGER.info("The selected volume path for db backup: {}", result);
        return result;
    }

    private void cleanUpFormerBackups(StackDto stackDto) throws CloudbreakOrchestratorFailedException {
        OrchestratorStateParams stateParams = createStateParams(stackDto, null, true);
        String command = String.format("ls -d %s* /var %s | xargs -I %% rm -rf %%/tmp/postgres_upgrade_backup",
                VolumeUtils.VOLUME_PREFIX, VolumeUtils.DATABASE_VOLUME);
        hostOrchestrator.runCommandOnHosts(List.of(stateParams.getPrimaryGatewayConfig()), stateParams.getTargetHostNames(), command);
    }

    private OrchestratorStateParams createStateParams(StackDto stackDto, String saltState, boolean onlyOnPrimary) {
        return saltStateParamsService.createStateParams(stackDto, saltState, onlyOnPrimary, MAX_RETRY, MAX_RETRY_ON_ERROR);
    }

    private OrchestratorStateParams createStateParams(Long stackId, String saltState, boolean onlyOnPrimary) {
        StackDto stack = stackDtoService.getById(stackId);
        return saltStateParamsService.createStateParams(stack, saltState, onlyOnPrimary, MAX_RETRY, MAX_RETRY_ON_ERROR);
    }

    private void logErrorAndThrow(String errorMessage) throws CloudbreakOrchestratorFailedException {
        LOGGER.warn(errorMessage);
        throw new CloudbreakOrchestratorFailedException(errorMessage);
    }
}