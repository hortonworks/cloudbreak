package com.sequenceiq.datalake.flow.datalake.restartservices.handler;

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
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.datalake.restartservices.DatalakeRestartServicesFlowEvent;
import com.sequenceiq.datalake.flow.datalake.restartservices.event.DatalakeRestartServicesFailedEvent;
import com.sequenceiq.datalake.flow.datalake.restartservices.event.DatalakeRestartServicesWaitEvent;
import com.sequenceiq.datalake.service.sdx.CloudbreakPoller;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class DatalakeRestartServicesWaitHandler extends ExceptionCatcherEventHandler<DatalakeRestartServicesWaitEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeRestartServicesWaitHandler.class);

    @Value("${sdx.datalake.restart_services.sleeptime_sec:5}")
    private int sleepTimeInSec;

    @Value("${sdx.datalake.restart_services.duration_min:30}")
    private int durationInMinutes;

    @Inject
    private SdxService sdxService;

    @Inject
    private CloudbreakPoller poller;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(DatalakeRestartServicesWaitEvent.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DatalakeRestartServicesWaitEvent> event) {
        return new DatalakeRestartServicesFailedEvent(resourceId, event.getData().getUserId(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<DatalakeRestartServicesWaitEvent> event) {
        DatalakeRestartServicesWaitEvent sdxEvent = event.getData();
        Long sdxId = sdxEvent.getResourceId();
        String userId = sdxEvent.getUserId();
        Selectable response;
        try {
            LOGGER.debug("Start polling datalake restart services process for id: {}", sdxId);
            SdxCluster sdxCluster = sdxService.getById(sdxId);
            PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes, TimeUnit.MINUTES);
            poller.pollServiceRestartUntilAvailable(sdxCluster, pollingConfig);
            response = new SdxEvent(DatalakeRestartServicesFlowEvent.DATALAKE_RESTART_SERVICES_FINISHED_EVENT.event(), sdxId, userId);
        } catch (UserBreakException userBreakException) {
            LOGGER.error("Datalake restart services polling exited before timeout. Cause: ", userBreakException);
            response = new DatalakeRestartServicesFailedEvent(sdxId, userId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.error("Datalake restart services poller stopped for stack: {}", sdxId);
            response = new DatalakeRestartServicesFailedEvent(sdxId, userId,
                    new PollerStoppedException("Datalake restart services timed out after " + durationInMinutes + " minutes"));
        } catch (PollerException exception) {
            LOGGER.error("Datalake restart services polling failed for stack: {}", sdxId);
            response = new DatalakeRestartServicesFailedEvent(sdxId, userId, exception);
        }
        return response;
    }

}
