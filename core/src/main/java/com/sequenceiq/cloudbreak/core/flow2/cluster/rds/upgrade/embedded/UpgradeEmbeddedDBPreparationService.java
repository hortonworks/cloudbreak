package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.embedded;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;

import java.util.Objects;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.service.StackUpdater;

@Service
public class UpgradeEmbeddedDBPreparationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeEmbeddedDBPreparationService.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    public void prepareEmbeddedDbUpgrade(Long stackId) {
        setStatusAndNotify(stackId, UPDATE_IN_PROGRESS, DetailedStackStatus.DATABASE_UPGRADE_IN_PROGRESS, "Prepare embedded database upgrade",
                ResourceEvent.CLUSTER_PREPARE_EMBEDDEDDB_UPGRADE_IN_PROGRESS);
    }

    public void prepareEmbeddedDbUpgradeFinished(Long stackId, Long clusterId) {
        String statusReason = "Prepare embedded database upgrade finished";
        LOGGER.debug(statusReason);
        InMemoryStateStore.deleteStack(stackId);
        InMemoryStateStore.deleteCluster(clusterId);
        setStatusAndNotify(stackId, AVAILABLE, DetailedStackStatus.AVAILABLE, statusReason, ResourceEvent.CLUSTER_PREPARE_EMBEDDEDDB_UPGRADE_FINISHED);
    }

    public void prepareEmbeddedDatabaseUpgradeFailed(Long stackId, Long clusterId, Exception exception) {
        String statusReason = "Prepare embedded database upgrade failed with exception " + exception.getMessage();
        LOGGER.debug(statusReason);
        InMemoryStateStore.deleteStack(stackId);
        if (Objects.nonNull(clusterId)) {
            InMemoryStateStore.deleteCluster(clusterId);
        }
        setStatusAndNotify(stackId, UPDATE_FAILED, DetailedStackStatus.PREPARE_EMBEDDED_UPGRADE_FAILED, statusReason, ResourceEvent.CLUSTER_RDS_UPGRADE_FAILED,
                statusReason);
    }

    private void setStatusAndNotify(Long stackId, Status status, DetailedStackStatus detailedStackStatus, String statusReason, ResourceEvent resourceEvent,
            String... args) {
        LOGGER.debug(statusReason);
        stackUpdater.updateStackStatus(stackId, detailedStackStatus, statusReason);
        flowMessageService.fireEventAndLog(stackId, status.name(), resourceEvent, args);
    }
}
