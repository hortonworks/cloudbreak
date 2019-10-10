package com.sequenceiq.datalake.flow.start.handler;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.datalake.flow.start.event.SdxStartFailedEvent;
import com.sequenceiq.datalake.flow.start.event.SdxStartSuccessEvent;
import com.sequenceiq.datalake.flow.start.event.SdxStartWaitRequest;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.start.SdxStartService;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class SdxStartWaitHandler implements EventHandler<SdxStartWaitRequest> {

    public static final int SLEEP_TIME_IN_SEC = 20;

    public static final int DURATION_IN_MINUTES = 40;

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxStartWaitHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private SdxStartService sdxStartService;

    @Override
    public String selector() {
        return "SdxStartWaitRequest";
    }

    @Override
    public void accept(Event<SdxStartWaitRequest> event) {
        SdxStartWaitRequest waitRequest = event.getData();
        Long sdxId = waitRequest.getResourceId();
        String userId = waitRequest.getUserId();
        String requestId = waitRequest.getRequestId();
        MDCBuilder.addRequestId(requestId);
        Selectable response;
        try {
            LOGGER.debug("Start polling stack deletion process for id: {}", sdxId);
            PollingConfig pollingConfig = new PollingConfig(SLEEP_TIME_IN_SEC, TimeUnit.SECONDS, DURATION_IN_MINUTES, TimeUnit.MINUTES);
            sdxStartService.waitCloudbreakCluster(sdxId, pollingConfig);
            response = new SdxStartSuccessEvent(sdxId, userId, requestId);
        } catch (UserBreakException userBreakException) {
            LOGGER.info("Start polling exited before timeout. Cause: ", userBreakException);
            response = new SdxStartFailedEvent(sdxId, userId, requestId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.info("Start poller stopped for stack: {}", sdxId);
            response = new SdxStartFailedEvent(sdxId, userId, requestId,
                    new PollerStoppedException("Datalake start timed out after " + DURATION_IN_MINUTES + " minutes"));
        } catch (PollerException exception) {
            LOGGER.info("Start polling failed for stack: {}", sdxId);
            response = new SdxStartFailedEvent(sdxId, userId, requestId, exception);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
