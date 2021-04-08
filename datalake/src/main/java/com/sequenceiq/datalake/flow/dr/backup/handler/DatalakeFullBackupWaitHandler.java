package com.sequenceiq.datalake.flow.dr.backup.handler;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.exception.CloudbreakApiException;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeBackupFailedEvent;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeBackupSuccessEvent;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeFullBackupWaitRequest;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.dr.SdxBackupRestoreService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import reactor.bus.Event;

@Component
public class DatalakeFullBackupWaitHandler extends ExceptionCatcherEventHandler<DatalakeFullBackupWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeFullBackupWaitHandler.class);

    @Value("${sdx.stack.backup.status.sleeptime_sec:20}")
    private int sleepTimeInSec;

    @Value("${sdx.stack.backup.status.duration_min:90}")
    private int durationInMinutes;

    @Inject
    private SdxBackupRestoreService sdxBackupRestoreService;

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
        Selectable response;
        try {
            LOGGER.info("Start polling datalake full backup status for id: {}", sdxId);
            PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes,
                TimeUnit.MINUTES);
            sdxBackupRestoreService.waitForDatalakeDrBackupToComplete(sdxId, request.getOperationId(), request.getUserId(),
                pollingConfig, "Full backup");
            response = new DatalakeBackupSuccessEvent(sdxId, userId, request.getOperationId());
        } catch (UserBreakException userBreakException) {
            LOGGER.info("Full backup polling exited before timeout. Cause: ", userBreakException);
            response = new DatalakeBackupFailedEvent(sdxId, userId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.info("Full backup poller stopped for cluster: {}", sdxId);
            response = new DatalakeBackupFailedEvent(sdxId, userId,
                new PollerStoppedException("Database backup timed out after " + durationInMinutes + " minutes"));
        } catch (PollerException exception) {
            LOGGER.info("Full backup polling failed for cluster: {}", sdxId);
            response = new DatalakeBackupFailedEvent(sdxId, userId, exception);
        } catch (CloudbreakApiException exception) {
            LOGGER.info("Datalake backup failed. Reason: " + exception.getMessage());
            response = new DatalakeBackupFailedEvent(sdxId, userId, exception);
        }
        return response;
    }
}
