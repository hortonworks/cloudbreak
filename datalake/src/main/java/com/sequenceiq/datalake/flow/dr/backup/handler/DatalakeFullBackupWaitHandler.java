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
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeBackupStatusResponse;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.exception.CloudbreakApiException;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeBackupCancelledEvent;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeBackupFailedEvent;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeBackupSuccessEvent;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeFullBackupWaitRequest;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.dr.BackupRestoreTimeoutService;
import com.sequenceiq.datalake.service.sdx.dr.SdxBackupRestoreService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class DatalakeFullBackupWaitHandler extends ExceptionCatcherEventHandler<DatalakeFullBackupWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeFullBackupWaitHandler.class);

    @Value("${sdx.stack.backup.status.sleeptime_sec:20}")
    private int sleepTimeInSec;

    @Value("${sdx.stack.backup.status.duration_min:240}")
    private int durationInMinutes;

    @Value("${sdx.stack.backup.status.long_duration_min:840}")
    private int longDurationInMinutes;

    @Inject
    private SdxBackupRestoreService sdxBackupRestoreService;

    @Inject
    private BackupRestoreTimeoutService backupRestoreTimeoutService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(DatalakeFullBackupWaitRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DatalakeFullBackupWaitRequest> event) {
        return new DatalakeBackupFailedEvent(resourceId, null, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<DatalakeFullBackupWaitRequest> event) {
        DatalakeFullBackupWaitRequest request = event.getData();
        Long sdxId = request.getResourceId();
        String userId = request.getUserId();
        int duration = backupRestoreTimeoutService.getBackupTimeout(sdxId, request.getFullDrMaxDurationInMin(), durationInMinutes, longDurationInMinutes);
        LOGGER.info("Timeout duration for full backup set to {} minutes", duration);

        Selectable response;
        try {
            LOGGER.info("Start polling datalake full backup status for id: {} with timeout duration: {}", sdxId, duration);
            PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, duration,
                    TimeUnit.MINUTES);
            DatalakeBackupStatusResponse backupStatusResponse = sdxBackupRestoreService.waitForDatalakeDrBackupToComplete(
                    sdxId, request.getOperationId(), request.getUserId(),
                    pollingConfig, "Full backup");
            switch (backupStatusResponse.getState()) {
                case CANCELLED:
                    response = new DatalakeBackupCancelledEvent(sdxId, userId, request.getOperationId());
                    break;
                case SUCCESSFUL, VALIDATION_SUCCESSFUL:
                    response = new DatalakeBackupSuccessEvent(sdxId, userId, request.getOperationId());
                    break;
                // failed states are handled via Exception catch block
                default:
                    LOGGER.warn("Unknown datalake backup status, assuming successful backup: {}", backupStatusResponse.getState());
                    response = new DatalakeBackupSuccessEvent(sdxId, userId, request.getOperationId());
            }
        } catch (UserBreakException userBreakException) {
            LOGGER.info("Full backup polling exited before timeout. Cause: ", userBreakException);
            response = new DatalakeBackupFailedEvent(sdxId, userId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.info("Full backup poller stopped for cluster: {}", sdxId);
            response = new DatalakeBackupFailedEvent(sdxId, userId,
                    new PollerStoppedException("Datalake backup timed out after " + duration + " minutes"));
        } catch (PollerException exception) {
            LOGGER.info("Full backup polling failed for cluster: {}", sdxId);
            response = new DatalakeBackupFailedEvent(sdxId, userId, exception);
        } catch (CloudbreakApiException exception) {
            LOGGER.info("Datalake backup failed. Reason: {}", exception.getMessage());
            response = new DatalakeBackupFailedEvent(sdxId, userId, exception);
        }
        return response;
    }

}
