package com.sequenceiq.cloudbreak.reactor.handler.cluster.dr.backup;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.backup.DatabaseBackupFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.backup.DatabaseBackupRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.backup.DatabaseBackupSuccess;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseBackupHandler extends ExceptionCatcherEventHandler<DatabaseBackupRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseBackupHandler.class);

    @Override
    public String selector() {
        return "DatabaseBackupRequest";
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e) {
        return new DatabaseBackupFailedEvent(resourceId, e, DetailedStackStatus.DATABASE_BACKUP_FAILED);
    }

    @Override
    protected void doAccept(HandlerEvent event) {
        LOGGER.info("HER DatabaseBackupHandler.doAccept");
        LOGGER.debug("Accepting Database backup event...");
        DatabaseBackupRequest request = event.getData();
        String backupLocation = request.getBackupLocation();
        LOGGER.info("HER DatabaseBackupHandler.doAccept backupLocation " + backupLocation);
        Selectable result;
        try {
            result = new DatabaseBackupSuccess(request.getResourceId());
        } catch (Exception e) {
            LOGGER.info("Database upgrade event failed", e);
            result = new DatabaseBackupFailedEvent(request.getResourceId(), e, DetailedStackStatus.DATABASE_BACKUP_FAILED);
        }
        sendEvent(result, event);
    }
}
