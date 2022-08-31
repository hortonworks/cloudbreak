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
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.upgrade.rds.DatabaseUpgradeBackupRestoreChecker;

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
    private DatabaseUpgradeBackupRestoreChecker backupRestoreValidator;

    public void pushSaltStates(Long stackId) {
        setStatusAndNotify(stackId, UPDATE_IN_PROGRESS, DetailedStackStatus.EXTERNAL_DATABASE_UPGRADE_VALIDATION_IN_PROGRESS,
                getMessage(ResourceEvent.CLUSTER_RDS_UPGRADE_VALIDATION_PUSH_SALT_STATES), ResourceEvent.CLUSTER_RDS_UPGRADE_VALIDATION_PUSH_SALT_STATES);
    }

    public void validateBackup(Long stackId) {
        setStatusAndNotify(stackId, UPDATE_IN_PROGRESS, DetailedStackStatus.EXTERNAL_DATABASE_UPGRADE_VALIDATION_IN_PROGRESS,
                getMessage(ResourceEvent.CLUSTER_RDS_UPGRADE_VALIDATION_BACKUP_VALIDATION), ResourceEvent.CLUSTER_RDS_UPGRADE_VALIDATION_BACKUP_VALIDATION);
    }

    public void validateRdsUpgradeFinished(Long stackId, Long clusterId) {
        String statusReason = "Validate RDS upgrade finished";
        LOGGER.debug(statusReason);
        InMemoryStateStore.deleteStack(stackId);
        InMemoryStateStore.deleteCluster(clusterId);
        setStatusAndNotify(stackId, AVAILABLE, DetailedStackStatus.EXTERNAL_DATABASE_UPGRADE_VALIDATION_FINISHED,
                statusReason, ResourceEvent.CLUSTER_RDS_UPGRADE_VALIDATION_FINISHED);
    }

    public void validateRdsUpgradeFailed(Long stackId, Long clusterId, Exception exception) {
        String statusReason = "Validate RDS upgrade failed with exception " + exception.getMessage();
        LOGGER.debug(statusReason);
        InMemoryStateStore.deleteStack(stackId);
        if (Objects.nonNull(clusterId)) {
            InMemoryStateStore.deleteCluster(clusterId);
        }
        setStatusAndNotify(stackId, UPDATE_FAILED, DetailedStackStatus.EXTERNAL_DATABASE_UPGRADE_VALIDATION_FAILED,
                statusReason, ResourceEvent.CLUSTER_RDS_UPGRADE_VALIDATION_FAILED);
    }

    public boolean shouldRunDataBackupRestore(ClusterViewContext context) {
        return backupRestoreValidator.shouldRunDataBackupRestore(context);
    }

    private void setStatusAndNotify(Long stackId, Status status, DetailedStackStatus detailedStackStatus, String statusReason, ResourceEvent resourceEvent) {
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, detailedStackStatus, statusReason);
        flowMessageService.fireEventAndLog(stackId, status.name(), resourceEvent);
    }

    private String getMessage(ResourceEvent resourceEvent) {
        return messagesService.getMessage(resourceEvent.getMessage());
    }
}

