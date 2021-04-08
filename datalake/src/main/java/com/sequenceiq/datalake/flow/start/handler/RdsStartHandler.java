package com.sequenceiq.datalake.flow.start.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.flow.start.event.RdsStartSuccessEvent;
import com.sequenceiq.datalake.flow.start.event.RdsWaitingToStartRequest;
import com.sequenceiq.datalake.flow.start.event.SdxStartFailedEvent;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.pause.DatabasePauseSupportService;
import com.sequenceiq.datalake.service.sdx.database.DatabaseService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;

import reactor.bus.Event;

@Component
public class RdsStartHandler extends ExceptionCatcherEventHandler<RdsWaitingToStartRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdsStartHandler.class);

    @Value("${sdx.db.operation.duration_min:60}")
    private int durationInMinutes;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private DatabaseService databaseService;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private DatabasePauseSupportService databasePauseSupportService;

    @Override
    public String selector() {
        return RdsWaitingToStartRequest.class.getSimpleName();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<RdsWaitingToStartRequest> event) {
        return new SdxStartFailedEvent(resourceId, null, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent event) {
        RdsWaitingToStartRequest rdsWaitRequest = event.getData();
        Long sdxId = rdsWaitRequest.getResourceId();
        String userId = rdsWaitRequest.getUserId();
        Selectable response;
        try {
            sdxClusterRepository.findById(sdxId).ifPresent(sdxCluster -> {
                if (databasePauseSupportService.isDatabasePauseSupported(sdxCluster)) {
                    sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.EXTERNAL_DATABASE_START_IN_PROGRESS,
                            "External database start in progress", sdxCluster);
                    LOGGER.debug("start polling database start for sdx: {}", sdxId);
                    databaseService.start(sdxCluster);
                } else {
                    LOGGER.debug("skipping start of database for sdx: {}", sdxId);
                }
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.EXTERNAL_DATABASE_STARTED,
                        "External database started", sdxCluster);
            });
            response = new RdsStartSuccessEvent(sdxId, userId);
        } catch (UserBreakException userBreakException) {
            LOGGER.error("Database polling exited before timeout. Cause: ", userBreakException);
            response = new SdxStartFailedEvent(sdxId, userId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.error("Database poller stopped for sdx: {}", sdxId, pollerStoppedException);
            response = new SdxStartFailedEvent(sdxId, userId,
                    new PollerStoppedException("Database start timed out after " + durationInMinutes + " minutes"));
        } catch (PollerException exception) {
            LOGGER.error("Database polling failed for sdx: {}", sdxId, exception);
            response = new SdxStartFailedEvent(sdxId, userId, exception);
        } catch (Exception anotherException) {
            LOGGER.error("Something wrong happened in sdx database start wait phase", anotherException);
            response = new SdxStartFailedEvent(sdxId, userId, anotherException);
        }
        return response;
    }
}
