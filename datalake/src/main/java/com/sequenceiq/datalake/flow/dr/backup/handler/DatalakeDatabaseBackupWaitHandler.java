package com.sequenceiq.datalake.flow.dr.backup.handler;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.entity.operation.SdxOperationStatus;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeDatabaseBackupFailedEvent;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeDatabaseBackupWaitRequest;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeFullBackupInProgressEvent;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.dr.BackupRestoreTimeoutService;
import com.sequenceiq.datalake.service.sdx.dr.SdxBackupRestoreService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class DatalakeDatabaseBackupWaitHandler extends ExceptionCatcherEventHandler<DatalakeDatabaseBackupWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeDatabaseBackupWaitHandler.class);

    @Value("${sdx.stack.backup.status.sleeptime_sec:20}")
    private int sleepTimeInSec;

    @Value("${sdx.stack.backup.status.duration_min:90}")
    private int durationInMinutes;

    @Inject
    private SdxBackupRestoreService sdxBackupRestoreService;

    @Inject
    private BackupRestoreTimeoutService backupRestoreTimeoutService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(DatalakeDatabaseBackupWaitRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DatalakeDatabaseBackupWaitRequest> event) {
        return new DatalakeDatabaseBackupFailedEvent(resourceId, null, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<DatalakeDatabaseBackupWaitRequest> event) {
        DatalakeDatabaseBackupWaitRequest request = event.getData();
        Long sdxId = request.getResourceId();
        String userId = request.getUserId();
        int duration = backupRestoreTimeoutService.getBackupTimeout(sdxId, request.getDatabaseMaxDurationInMin(), durationInMinutes, 0);
        LOGGER.info("Timeout duration for database backup set to {} minutes.", duration);
        Selectable response;
        try {
            LOGGER.info("Start polling datalake database backup for id: {} with timeout duration: {}", sdxId, duration);
            PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, duration, TimeUnit.MINUTES);
            sdxBackupRestoreService.waitCloudbreakFlow(sdxId, pollingConfig, "Database backup");
            response = new DatalakeFullBackupInProgressEvent(sdxId, userId, request.getOperationId());
        } catch (UserBreakException userBreakException) {
            LOGGER.info("Database backup polling exited before timeout. Cause: ", userBreakException);
            sdxBackupRestoreService.updateDatabaseStatusEntry(event.getData().getOperationId(),
                    SdxOperationStatus.FAILED, userBreakException.getLocalizedMessage());
            response = new DatalakeDatabaseBackupFailedEvent(sdxId, userId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.info("Database backup poller stopped for cluster: {}", sdxId);
            sdxBackupRestoreService.updateDatabaseStatusEntry(event.getData().getOperationId(),
                    SdxOperationStatus.FAILED, pollerStoppedException.getLocalizedMessage());
            String errorStage = sdxBackupRestoreService.createDatabaseBackupRestoreErrorStage(sdxId);
            response = new DatalakeDatabaseBackupFailedEvent(sdxId, userId,
                    new PollerStoppedException("Database backup timed out after " + duration + " minutes" + errorStage));
        } catch (PollerException exception) {
            LOGGER.info("Database backup polling failed for cluster: {}", sdxId);
            sdxBackupRestoreService.updateDatabaseStatusEntry(event.getData().getOperationId(),
                    SdxOperationStatus.FAILED, exception.getLocalizedMessage());
            response = new DatalakeDatabaseBackupFailedEvent(sdxId, userId, exception);
        }
        return response;
    }
}
