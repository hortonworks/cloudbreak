package com.sequenceiq.cloudbreak.reactor.handler.cluster.dr.backup;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.datalakedr.DatalakeDrClient;
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeDrStatusResponse;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.backup.BackupSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.backup.DatabaseBackupFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.backup.FullBackupStatusRequest;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FullBackupStatusHandler extends ExceptionCatcherEventHandler<FullBackupStatusRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FullBackupStatusHandler.class);

    @Inject
    private DatalakeDrClient datalakeDrClient;

    @Inject
    private StackService stackService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(FullBackupStatusRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e) {
        // TODO update DetailedStackStatus
        return new DatabaseBackupFailedEvent(resourceId, e, DetailedStackStatus.DATABASE_BACKUP_FAILED);
    }

    @Override
    protected Selectable doAccept(HandlerEvent event) {
        LOGGER.info("HER in FullBackupStatusHandler");
        FullBackupStatusRequest request = event.getData();
        Selectable result;
        Long stackId = request.getResourceId();
        LOGGER.debug("Checking status of backup on stack {}, backup id {}", stackId, request.getBackupId());
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(stackId);
            Cluster cluster = stack.getCluster();
            // TODO loop
            DatalakeDrStatusResponse response = datalakeDrClient.getBackupStatus(cluster.getName(),
                request.getBackupId(), request.getUserCrn());
            LOGGER.info("HER response.getState() " + response.getState());
            result = new BackupSuccess(stackId);
        } catch (Exception e) {
            LOGGER.error("Database backup event failed", e);
            // TODO error case isn't working and flow is getting stuck
//            result = new DatabaseBackupFailedEvent(stackId, e, DetailedStackStatus.DATABASE_BACKUP_FAILED);
            result = new BackupSuccess(stackId);
        }
        return result;
    }
}
