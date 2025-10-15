package com.sequenceiq.datalake.flow.dr.validation.handler;

import static com.sequenceiq.datalake.flow.dr.validation.DatalakeRestoreValidationEvent.DATALAKE_RESTORE_VALIDATION_SUCCESS_EVENT;

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
import com.sequenceiq.datalake.flow.dr.validation.event.DatalakeRestoreValidationFailedEvent;
import com.sequenceiq.datalake.flow.dr.validation.event.DatalakeRestoreValidationWaitRequest;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.dr.SdxBackupRestoreService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class DatalakeRestoreValidationWaitHandler extends ExceptionCatcherEventHandler<DatalakeRestoreValidationWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeRestoreValidationWaitHandler.class);

    @Value("${sdx.stack.restore.validation.status.sleeptime_sec:20}")
    private int sleepTimeInSec;

    @Value("${sdx.stack.restore.validation.status.duration_min:20}")
    private int durationInMinutes;

    @Inject
    private SdxBackupRestoreService sdxBackupRestoreService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(DatalakeRestoreValidationWaitRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DatalakeRestoreValidationWaitRequest> event) {
        return new DatalakeRestoreValidationFailedEvent(resourceId, null, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<DatalakeRestoreValidationWaitRequest> event) {
        DatalakeRestoreValidationWaitRequest request = event.getData();
        Long sdxId = request.getResourceId();
        String userId = request.getUserId();
        Selectable response;
        try {
            LOGGER.info("Start polling datalake restore validation status for id: {}", sdxId);
            PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes,
                    TimeUnit.MINUTES);
            sdxBackupRestoreService.waitForDatalakeDrRestoreToComplete(sdxId, request.getOperationId(), request.getUserId(),
                    pollingConfig, "Restore validation");
            response = new SdxEvent(DATALAKE_RESTORE_VALIDATION_SUCCESS_EVENT.event(), sdxId, userId);
        } catch (UserBreakException userBreakException) {
            LOGGER.info("Restore validation polling exited before timeout. Cause: ", userBreakException);
            response = new DatalakeRestoreValidationFailedEvent(sdxId, userId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.info("Restore validation poller timed out for cluster: {}", sdxId);
            response = new DatalakeRestoreValidationFailedEvent(sdxId, userId,
                    new PollerStoppedException("Datalake restore validation timed out after " + durationInMinutes + " minutes"));
        } catch (PollerException exception) {
            LOGGER.info("Restore validation polling failed for cluster: {}", sdxId);
            response = new DatalakeRestoreValidationFailedEvent(sdxId, userId, exception);
        } catch (CloudbreakServiceException exception) {
            LOGGER.info("Datalake restore validation failed. Reason: " + exception.getMessage());
            response = new DatalakeRestoreValidationFailedEvent(sdxId, userId, exception);
        }
        return response;
    }
}
