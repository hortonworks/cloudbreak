package com.sequenceiq.datalake.flow.stop.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.start.event.SdxStartFailedEvent;
import com.sequenceiq.datalake.flow.stop.SdxStopEvent;
import com.sequenceiq.datalake.flow.stop.event.SdxStopAllDatahubRequest;
import com.sequenceiq.datalake.service.sdx.stop.SdxStopService;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class SdxStopAllDatahubHandler implements EventHandler<SdxStopAllDatahubRequest> {

    public static final int SLEEP_TIME_IN_SEC = 20;

    public static final int DURATION_IN_MINUTES = 40;

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxStopAllDatahubHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private SdxStopService sdxStopService;

    @Override
    public String selector() {
        return "SdxStopAllDatahubRequest";
    }

    @Override
    public void accept(Event<SdxStopAllDatahubRequest> event) {
        SdxStopAllDatahubRequest stopAllDatahubRequest = event.getData();
        Long sdxId = stopAllDatahubRequest.getResourceId();
        String userId = stopAllDatahubRequest.getUserId();
        Selectable response;
        try {
            LOGGER.debug("Polling is started for the operation of stopping all datahubs clusters for sdx with id: {}", sdxId);
            sdxStopService.stopAllDatahub(sdxId);
            response = new SdxEvent(SdxStopEvent.SDX_STOP_IN_PROGRESS_EVENT.event(), sdxId, userId);
        } catch (UserBreakException userBreakException) {
            LOGGER.info("Polling exited before timeout. Cause ", userBreakException);
            response = new SdxStartFailedEvent(sdxId, userId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.info("Poller stopped for stack: " + sdxId, pollerStoppedException);
            response = new SdxStartFailedEvent(sdxId, userId,
                    new PollerStoppedException("Datalake start timed out after " + DURATION_IN_MINUTES + " minutes", pollerStoppedException));
        } catch (PollerException exception) {
            LOGGER.info("Polling failed for stack: {}", sdxId);
            response = new SdxStartFailedEvent(sdxId, userId, exception);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
