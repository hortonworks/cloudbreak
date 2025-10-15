package com.sequenceiq.datalake.flow.dr.backup.handler;

import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupEvent.DATALAKE_BACKUP_SERVICES_STOPPED_EVENT;

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
import com.sequenceiq.datalake.entity.operation.SdxOperation;
import com.sequenceiq.datalake.entity.operation.SdxOperationType;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeBackupAwaitServicesStoppedRequest;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeBackupFailedEvent;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeDatabaseBackupStartEvent;
import com.sequenceiq.datalake.repository.SdxOperationRepository;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.dr.SdxBackupRestoreService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class DatalakeBackupAwaitServicesStoppedHandler extends ExceptionCatcherEventHandler<DatalakeBackupAwaitServicesStoppedRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeBackupAwaitServicesStoppedHandler.class);

    @Value("${sdx.stack.backup.status.sleeptime_sec:4}")
    private int sleepTimeInSec;

    @Value("${sdx.stack.backup.status.duration_min:10}")
    private int durationInMinutes;

    @Inject
    private SdxBackupRestoreService sdxBackupRestoreService;

    @Inject
    private SdxOperationRepository sdxOperationRepository;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(DatalakeBackupAwaitServicesStoppedRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DatalakeBackupAwaitServicesStoppedRequest> event) {
        return new DatalakeBackupFailedEvent(resourceId, null, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<DatalakeBackupAwaitServicesStoppedRequest> event) {
        DatalakeBackupAwaitServicesStoppedRequest request = event.getData();
        SdxOperation drStatus = request.getDrStatus();
        Long sdxId = request.getResourceId();
        String userId = request.getUserId();
        String operationId = request.getOperationId();

        try {
            SdxOperation sdxOperation = sdxOperationRepository.findSdxOperationByOperationId(operationId);
            if (sdxOperation != null) {
                LOGGER.info("The database entry with operation id: {}, status: {}, operation type: {}, and cluster id: {} already exists",
                        sdxOperation.getOperationId(),
                        sdxOperation.getStatus(),
                        sdxOperation.getOperationType(),
                        sdxOperation.getSdxClusterId());
            }
        } catch (Exception e) {
            LOGGER.error("Error occurred while retrieving operation with id: {}", operationId, e);
        }

        Selectable response;
        try {
            sdxOperationRepository.save(request.getDrStatus());

            LOGGER.info("Start polling datalake status for id: {} with timeout duration: {}", sdxId, durationInMinutes);
            PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes,
                    TimeUnit.MINUTES);
            sdxBackupRestoreService.waitForServiceToBeStopped(
                    sdxId, operationId, userId, pollingConfig, "Backup services stopped", SdxOperationType.BACKUP
            );
            response = new DatalakeDatabaseBackupStartEvent(DATALAKE_BACKUP_SERVICES_STOPPED_EVENT.event(),
                    drStatus,
                    userId,
                    operationId,
                    request.getBackupLocation(),
                    request.getSkipDatabaseNames(),
                    0);
        } catch (UserBreakException userBreakException) {
            LOGGER.info("Backup services stopped polling exited before timeout. Cause: ", userBreakException);
            response = new DatalakeBackupFailedEvent(sdxId, userId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.info("Backup services stopped poller stopped for cluster: {}", sdxId);
            response = new DatalakeBackupFailedEvent(sdxId, userId,
                    new PollerStoppedException("Datalake backup timed out after " + durationInMinutes + " minutes"));
        } catch (PollerException exception) {
            LOGGER.info("Backup services stopped polling failed for cluster: {}", sdxId);
            response = new DatalakeBackupFailedEvent(sdxId, userId, exception);
        } catch (CloudbreakServiceException exception) {
            LOGGER.info("Datalake services stopped failed. Reason: " + exception.getMessage());
            response = new DatalakeBackupFailedEvent(sdxId, userId, exception);
        }

        return response;
    }
}
