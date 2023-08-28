package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;

import java.util.Objects;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.upgrade.rds.DatabaseUpgradeBackupRestoreChecker;
import com.sequenceiq.cloudbreak.service.upgrade.rds.RdsUpgradeOrchestratorService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;

@Service
public class UpgradeRdsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeRdsService.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private RdsUpgradeOrchestratorService rdsUpgradeOrchestratorService;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private DatabaseUpgradeBackupRestoreChecker backupRestoreChecker;

    @Inject
    private EntitlementService entitlementService;

    void stopServicesState(Long stackId) {
        setStatusAndNotify(stackId, getMessage(ResourceEvent.CLUSTER_RDS_UPGRADE_STOP_SERVICES), ResourceEvent.CLUSTER_RDS_UPGRADE_STOP_SERVICES);
    }

    void backupRdsState(Long stackId) {
        setStatusAndNotify(stackId, getMessage(ResourceEvent.CLUSTER_RDS_UPGRADE_BACKUP_DATA), ResourceEvent.CLUSTER_RDS_UPGRADE_BACKUP_DATA);
    }

    void upgradeRdsState(Long stackId) {
        setStatusAndNotify(stackId, getMessage(ResourceEvent.CLUSTER_RDS_UPGRADE_DBSERVER_UPGRADE), ResourceEvent.CLUSTER_RDS_UPGRADE_DBSERVER_UPGRADE);
    }

    void restoreRdsState(Long stackId) {
        setStatusAndNotify(stackId, getMessage(ResourceEvent.CLUSTER_RDS_UPGRADE_RESTORE_DATA), ResourceEvent.CLUSTER_RDS_UPGRADE_RESTORE_DATA);
    }

    void startServicesState(Long stackId) {
        setStatusAndNotify(stackId, getMessage(ResourceEvent.CLUSTER_RDS_UPGRADE_START_SERVICES), ResourceEvent.CLUSTER_RDS_UPGRADE_START_SERVICES);
    }

    void installPostgresPackagesState(Long stackId, String majorVersion) {
        setStatusAndNotify(stackId, getMessage(ResourceEvent.CLUSTER_RDS_UPGRADE_INSTALL_PG, majorVersion),
                ResourceEvent.CLUSTER_RDS_UPGRADE_INSTALL_PG, majorVersion);
    }

    private void setStatusAndNotify(Long stackId, String statusReason, ResourceEvent resourceEvent, String... args) {
        updateStatus(stackId, statusReason);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), resourceEvent, args);
    }

    private void setStatusAndNotify(Long stackId, String statusReason, ResourceEvent resourceEvent) {
        updateStatus(stackId, statusReason);
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), resourceEvent);
    }

    private void updateStatus(Long stackId, String statusReason) {
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.DATABASE_UPGRADE_IN_PROGRESS, statusReason);
    }

    public void backupRds(Long stackId, String backupLocation, String backupInstanceProfile) throws CloudbreakOrchestratorException {
        rdsUpgradeOrchestratorService.backupRdsData(stackId, backupLocation, backupInstanceProfile);
    }

    public void restoreRds(Long stackId, String targetVersion) throws CloudbreakOrchestratorException {
        rdsUpgradeOrchestratorService.restoreRdsData(stackId, targetVersion);
    }

    public void installPostgresPackages(Long stackId, MajorVersion targetVersion) throws CloudbreakOrchestratorException {
        rdsUpgradeOrchestratorService.installPostgresPackages(stackId, targetVersion);
        rdsUpgradeOrchestratorService.updatePostgresAlternatives(stackId, targetVersion);
    }

    public void handleInstallPostgresPackagesError(Long stackId, String version, String exception) {
        flowMessageService.fireEventAndLog(stackId, UPDATE_IN_PROGRESS.name(), ResourceEvent.CLUSTER_RDS_UPGRADE_INSTALL_PG_FAILED, version, exception);
    }

    public void rdsUpgradeFinished(Long stackId, Long clusterId) {
        String statusReason = "RDS upgrade finished";
        LOGGER.debug(statusReason);
        InMemoryStateStore.deleteStack(stackId);
        InMemoryStateStore.deleteCluster(clusterId);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.AVAILABLE, statusReason);
        flowMessageService.fireEventAndLog(stackId, AVAILABLE.name(), ResourceEvent.CLUSTER_RDS_UPGRADE_FINISHED);
    }

    public void rdsUpgradeFailed(Long stackId, Long clusterId, Exception exception) {
        String statusReason = "RDS upgrade failed with exception: " + exception.getMessage();
        LOGGER.warn("RDS upgrade failed with exception: ", exception);
        InMemoryStateStore.deleteStack(stackId);
        if (Objects.nonNull(clusterId)) {
            InMemoryStateStore.deleteCluster(clusterId);
        }
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.DATABASE_UPGRADE_FAILED, statusReason);
        flowMessageService.fireEventAndLog(stackId, UPDATE_FAILED.name(), ResourceEvent.CLUSTER_RDS_UPGRADE_FAILED, exception.getMessage());
    }

    public boolean shouldRunDataBackupRestore(StackView stack, ClusterView cluster) {
        return backupRestoreChecker.shouldRunDataBackupRestore(stack, cluster);
    }

    public boolean shouldStopStartServices(StackView stack) {
        return !entitlementService.isPostgresUpgradeSkipServicesAndCmStopEnabled(Crn.safeFromString(stack.getResourceCrn()).getAccountId());
    }

    private String getMessage(ResourceEvent resourceEvent) {
        return messagesService.getMessage(resourceEvent.getMessage());
    }

    private String getMessage(ResourceEvent resourceEvent, Object... args) {
        return messagesService.getMessageWithArgs(resourceEvent.getMessage(), args);
    }
}
