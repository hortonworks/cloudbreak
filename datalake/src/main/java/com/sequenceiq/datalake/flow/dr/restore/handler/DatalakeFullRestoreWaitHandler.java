package com.sequenceiq.datalake.flow.dr.restore.handler;

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
import com.sequenceiq.cloudbreak.exception.CloudbreakApiException;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeFullRestoreWaitRequest;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeRestoreFailedEvent;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeRestoreSuccessEvent;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.dr.BackupRestoreTimeoutService;
import com.sequenceiq.datalake.service.sdx.dr.SdxBackupRestoreService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class DatalakeFullRestoreWaitHandler extends ExceptionCatcherEventHandler<DatalakeFullRestoreWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeFullRestoreWaitHandler.class);

    @Value("${sdx.stack.restore.status.sleeptime_sec:20}")
    private int sleepTimeInSec;

    @Value("${sdx.stack.restore.status.duration_min:240}")
    private int durationInMinutes;

    @Inject
    private SdxBackupRestoreService sdxBackupRestoreService;

    @Inject
    private BackupRestoreTimeoutService backupRestoreTimeoutService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(DatalakeFullRestoreWaitRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DatalakeFullRestoreWaitRequest> event) {
        return new DatalakeRestoreFailedEvent(resourceId, null, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<DatalakeFullRestoreWaitRequest> event) {
        DatalakeFullRestoreWaitRequest request = event.getData();
        Long sdxId = request.getResourceId();
        String userId = request.getUserId();
        Selectable response;
        int duration = backupRestoreTimeoutService.getRestoreTimeout(sdxId, request.getFullDrMaxDurationInMin(), durationInMinutes, 0);
        LOGGER.info("Timeout duration for full restore set to {} minutes.", duration);
        try {
            LOGGER.info("Start polling datalake full restore status for id: {} with timeout duration: {}", sdxId, duration);
            PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, duration,
                TimeUnit.MINUTES);
            sdxBackupRestoreService.waitForDatalakeDrRestoreToComplete(sdxId, request.getOperationId(), request.getUserId(),
                pollingConfig, "Full restore");
            response = new DatalakeRestoreSuccessEvent(sdxId, userId, request.getOperationId());
        } catch (UserBreakException userBreakException) {
            LOGGER.info("Full restore polling exited before timeout. Cause: ", userBreakException);
            response = new DatalakeRestoreFailedEvent(sdxId, userId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.info("Full restore poller stopped for cluster: {}", sdxId);
            response = new DatalakeRestoreFailedEvent(sdxId, userId,
                new PollerStoppedException("Data lake restore timed out after " + durationInMinutes + " minutes"));
        } catch (PollerException exception) {
            LOGGER.info("Full restore polling failed for cluster: {}", sdxId);
            response = new DatalakeRestoreFailedEvent(sdxId, userId, exception);
        } catch (CloudbreakApiException exception) {
            LOGGER.info("Datalake restore failed. Reason: {}", exception.getMessage());
            response = new DatalakeRestoreFailedEvent(sdxId, userId, exception);
        }
        return response;
    }
}
