package com.sequenceiq.datalake.flow.stop.handler;

import static com.sequenceiq.datalake.service.sdx.database.DatabaseService.DURATION_IN_MINUTES_FOR_DB_POLLING;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.flow.stop.event.RdsStopSuccessEvent;
import com.sequenceiq.datalake.flow.stop.event.RdsWaitingToStopRequest;
import com.sequenceiq.datalake.flow.stop.event.SdxStopFailedEvent;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.pause.DatabasePauseSupportService;
import com.sequenceiq.datalake.service.sdx.database.DatabaseService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class RdsStopHandler extends ExceptionCatcherEventHandler<RdsWaitingToStopRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdsStopHandler.class);

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
        return RdsWaitingToStopRequest.class.getSimpleName();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<RdsWaitingToStopRequest> event) {
        return new SdxStopFailedEvent(resourceId, null, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<RdsWaitingToStopRequest> event) {
        RdsWaitingToStopRequest rdsWaitRequest = event.getData();
        Long sdxId = rdsWaitRequest.getResourceId();
        String userId = rdsWaitRequest.getUserId();
        Selectable response;
        try {
            sdxClusterRepository.findById(sdxId).ifPresent(sdxCluster -> {
                if (databasePauseSupportService.isDatabasePauseSupported(sdxCluster)) {
                    sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.EXTERNAL_DATABASE_STOP_IN_PROGRESS,
                            "External database stop in progress", sdxCluster);
                    LOGGER.debug("stop polling database stop for sdx: {}", sdxId);
                    databaseService.stop(sdxCluster);
                } else {
                    LOGGER.debug("skipping stop of database for sdx: {}", sdxId);
                }
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.EXTERNAL_DATABASE_STOPPED,
                        "External database stopped", sdxCluster);
            });
            response = new RdsStopSuccessEvent(sdxId, userId);
        } catch (UserBreakException userBreakException) {
            LOGGER.error("Database polling exited before timeout. Cause: ", userBreakException);
            response = new SdxStopFailedEvent(sdxId, userId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.error("Database poller stopped for sdx: {}", sdxId, pollerStoppedException);
            response = new SdxStopFailedEvent(sdxId, userId,
                    new PollerStoppedException("Database stop timed out after " + DURATION_IN_MINUTES_FOR_DB_POLLING + " minutes"));
        } catch (PollerException exception) {
            LOGGER.error("Database polling failed for sdx: {}", sdxId, exception);
            response = new SdxStopFailedEvent(sdxId, userId, exception);
        } catch (Exception anotherException) {
            LOGGER.error("Something wrong happened in sdx database stop wait phase", anotherException);
            response = new SdxStopFailedEvent(sdxId, userId, anotherException);
        }
        return response;
    }
}
