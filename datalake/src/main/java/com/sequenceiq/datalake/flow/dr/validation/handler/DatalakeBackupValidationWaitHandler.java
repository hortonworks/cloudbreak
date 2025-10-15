package com.sequenceiq.datalake.flow.dr.validation.handler;

import static com.sequenceiq.datalake.flow.dr.validation.DatalakeBackupValidationEvent.DATALAKE_BACKUP_VALIDATION_SUCCESS_EVENT;

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
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.dr.validation.event.DatalakeBackupValidationFailedEvent;
import com.sequenceiq.datalake.flow.dr.validation.event.DatalakeBackupValidationWaitRequest;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.dr.SdxBackupRestoreService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class DatalakeBackupValidationWaitHandler extends ExceptionCatcherEventHandler<DatalakeBackupValidationWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeBackupValidationWaitHandler.class);

    @Value("${sdx.stack.backup.status.sleeptime_sec:20}")
    private int sleepTimeInSec;

    @Value("${sdx.stack.backup.status.duration_min:240}")
    private int durationInMinutes;

    @Inject
    private SdxBackupRestoreService sdxBackupRestoreService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(DatalakeBackupValidationWaitRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DatalakeBackupValidationWaitRequest> event) {
        return new DatalakeBackupValidationFailedEvent(resourceId, null, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<DatalakeBackupValidationWaitRequest> event) {
        DatalakeBackupValidationWaitRequest request = event.getData();
        Long sdxId = request.getResourceId();
        String userId = request.getUserId();
        Selectable response;
        try {
            LOGGER.info("Start polling datalake full backup status for id: {}", sdxId);
            PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes,
                TimeUnit.MINUTES);
            sdxBackupRestoreService.waitForDatalakeDrBackupToComplete(sdxId, request.getOperationId(), request.getUserId(),
                pollingConfig, "Backup validation");
            response = new SdxEvent(DATALAKE_BACKUP_VALIDATION_SUCCESS_EVENT.event(), sdxId, userId);
        } catch (UserBreakException userBreakException) {
            LOGGER.info("Backup validation polling exited before timeout. Cause: ", userBreakException);
            response = new DatalakeBackupValidationFailedEvent(sdxId, userId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.info("Backup validation poller timed out for cluster: {}", sdxId);
            response = new DatalakeBackupValidationFailedEvent(sdxId, userId,
                new PollerStoppedException("Datalake backup validation timed out after " + durationInMinutes + " minutes"));
        } catch (PollerException exception) {
            LOGGER.info("Backup validation polling failed for cluster: {}", sdxId);
            response = new DatalakeBackupValidationFailedEvent(sdxId, userId, exception);
        } catch (CloudbreakServiceException exception) {
            LOGGER.info("Datalake backup validation failed. Reason: " + exception.getMessage());
            response = new DatalakeBackupValidationFailedEvent(sdxId, userId, exception);
        }
        return response;
    }
}
