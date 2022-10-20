package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.validation;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;

import java.util.Objects;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.upgrade.rds.DatabaseUpgradeBackupRestoreChecker;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;

@Service
public class ValidateRdsUpgradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidateRdsUpgradeService.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private DatabaseUpgradeBackupRestoreChecker backupRestoreChecker;

    void pushSaltStates(Long stackId) {
        setStatusAndNotify(stackId, UPDATE_IN_PROGRESS, DetailedStackStatus.EXTERNAL_DATABASE_UPGRADE_VALIDATION_IN_PROGRESS,
                getMessage(ResourceEvent.CLUSTER_RDS_UPGRADE_VALIDATION_PUSH_SALT_STATES), ResourceEvent.CLUSTER_RDS_UPGRADE_VALIDATION_PUSH_SALT_STATES);
    }

    void validateBackup(Long stackId) {
        setStatusAndNotify(stackId, UPDATE_IN_PROGRESS, DetailedStackStatus.EXTERNAL_DATABASE_UPGRADE_VALIDATION_IN_PROGRESS,
                getMessage(ResourceEvent.CLUSTER_RDS_UPGRADE_VALIDATION_BACKUP_VALIDATION), ResourceEvent.CLUSTER_RDS_UPGRADE_VALIDATION_BACKUP_VALIDATION);
    }

    void rdsUpgradeStarted(Long stackId, TargetMajorVersion version) {
        setStatusAndNotify(stackId, UPDATE_IN_PROGRESS, DetailedStackStatus.EXTERNAL_DATABASE_UPGRADE_VALIDATION_IN_PROGRESS,
                getMessage(ResourceEvent.CLUSTER_RDS_UPGRADE_STARTED, version.getMajorVersion()), ResourceEvent.CLUSTER_RDS_UPGRADE_STARTED,
                version.getMajorVersion());
    }

    void validateRdsUpgradeFinished(Long stackId, Long clusterId) {
        String statusReason = "Validate RDS upgrade finished";
        InMemoryStateStore.deleteStack(stackId);
        InMemoryStateStore.deleteCluster(clusterId);
        setStatusAndNotify(stackId, AVAILABLE, DetailedStackStatus.EXTERNAL_DATABASE_UPGRADE_VALIDATION_FINISHED,
                statusReason, ResourceEvent.CLUSTER_RDS_UPGRADE_VALIDATION_FINISHED);
    }

    void validateRdsUpgradeFailed(Long stackId, Long clusterId, Exception exception) {
        String statusReason = "Validate RDS upgrade failed with exception: " + exception.getMessage();
        LOGGER.warn("Validate RDS upgrade failed with exception: ", exception);
        InMemoryStateStore.deleteStack(stackId);
        if (Objects.nonNull(clusterId)) {
            InMemoryStateStore.deleteCluster(clusterId);
        }
        updateStatus(stackId, DetailedStackStatus.EXTERNAL_DATABASE_UPGRADE_VALIDATION_FAILED, statusReason);
        flowMessageService.fireEventAndLog(stackId, UPDATE_FAILED.name(), ResourceEvent.CLUSTER_RDS_UPGRADE_VALIDATION_FAILED, exception.getMessage());
    }

    boolean shouldRunDataBackupRestore(StackView stack, ClusterView cluster) {
        return backupRestoreChecker.shouldRunDataBackupRestore(stack, cluster);
    }

    private void setStatusAndNotify(Long stackId, Status status, DetailedStackStatus detailedStackStatus, String statusReason, ResourceEvent resourceEvent) {
        LOGGER.debug(statusReason);
        updateStatus(stackId, detailedStackStatus, statusReason);
        flowMessageService.fireEventAndLog(stackId, status.name(), resourceEvent);
    }

    private void updateStatus(Long stackId, DetailedStackStatus detailedStackStatus, String statusReason) {
        stackUpdater.updateStackStatus(stackId, detailedStackStatus, statusReason);
    }

    private void setStatusAndNotify(Long stackId, Status status, DetailedStackStatus detailedStackStatus, String statusReason, ResourceEvent resourceEvent,
                                    String... args) {
        LOGGER.debug(statusReason);
        updateStatus(stackId, detailedStackStatus, statusReason);
        flowMessageService.fireEventAndLog(stackId, status.name(), resourceEvent, args);
    }

    private String getMessage(ResourceEvent resourceEvent) {
        return messagesService.getMessage(resourceEvent.getMessage());
    }

    private String getMessage(ResourceEvent resourceEvent, Object... args) {
        return messagesService.getMessageWithArgs(resourceEvent.getMessage(), args);
    }
}

