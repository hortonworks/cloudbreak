package com.sequenceiq.datalake.flow.dr.restore.handler;

import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreEvent.DATALAKE_RESTORE_SERVICES_STOPPED_EVENT;

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
import com.sequenceiq.datalake.entity.operation.SdxOperation;
import com.sequenceiq.datalake.entity.operation.SdxOperationType;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeDatabaseRestoreStartEvent;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeRestoreAwaitServicesStoppedRequest;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeRestoreFailedEvent;
import com.sequenceiq.datalake.repository.SdxOperationRepository;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.dr.SdxBackupRestoreService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class DatalakeRestoreAwaitServicesStoppedHandler extends ExceptionCatcherEventHandler<DatalakeRestoreAwaitServicesStoppedRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeRestoreAwaitServicesStoppedHandler.class);

    @Value("${sdx.stack.restore.status.sleeptime_sec:4}")
    private int sleepTimeInSec;

    @Value("${sdx.stack.restore.status.duration_min:10}")
    private int durationInMinutes;

    @Inject
    private SdxBackupRestoreService sdxBackupRestoreService;

    @Inject
    private SdxOperationRepository sdxOperationRepository;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(DatalakeRestoreAwaitServicesStoppedRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DatalakeRestoreAwaitServicesStoppedRequest> event) {
        return new DatalakeRestoreFailedEvent(resourceId, null, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<DatalakeRestoreAwaitServicesStoppedRequest> event) {
        DatalakeRestoreAwaitServicesStoppedRequest request = event.getData();
        SdxOperation drStatus = request.getDrStatus();
        Long sdxId = request.getResourceId();
        String userId = request.getUserId();
        String operationId = request.getOperationId();
        if (event.getData().isValidationOnly()) {
            return  new DatalakeDatabaseRestoreStartEvent(DATALAKE_RESTORE_SERVICES_STOPPED_EVENT.event(),
                sdxId,
                userId,
                request.getBackupId(),
                operationId,
                request.getBackupLocation(),
                0,
                true);
        }
        Selectable response;
        try {
            sdxOperationRepository.save(request.getDrStatus());

            LOGGER.info("Start polling datalake status for id: {} with timeout duration: {}", sdxId, durationInMinutes);
            PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes,
                    TimeUnit.MINUTES);
            sdxBackupRestoreService.waitForServiceToBeStopped(
                    sdxId, operationId, userId, pollingConfig, "Restore services stopped", SdxOperationType.RESTORE
            );
            response = new DatalakeDatabaseRestoreStartEvent(DATALAKE_RESTORE_SERVICES_STOPPED_EVENT.event(),
                    sdxId,
                    userId,
                    request.getBackupId(),
                    operationId,
                    request.getBackupLocation(),
                    request.getDatabaseMaxDurationInMin(),
                    false);
        } catch (UserBreakException userBreakException) {
            LOGGER.info("Restore services stopped polling exited before timeout. Cause: ", userBreakException);
            response = new DatalakeRestoreFailedEvent(sdxId, userId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.info("Restore services stopped poller stopped for cluster: {}", sdxId);
            response = new DatalakeRestoreFailedEvent(sdxId, userId,
                    new PollerStoppedException("Datalake restore timed out after " + durationInMinutes + " minutes"));
        } catch (PollerException exception) {
            LOGGER.info("Restore services stopped polling failed for cluster: {}", sdxId);
            response = new DatalakeRestoreFailedEvent(sdxId, userId, exception);
        } catch (CloudbreakApiException exception) {
            LOGGER.info("Datalake restore failed. Reason: " + exception.getMessage());
            response = new DatalakeRestoreFailedEvent(sdxId, userId, exception);
        }

        return response;
    }
}
