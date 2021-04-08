package com.sequenceiq.datalake.flow.start.handler;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.flow.start.event.SdxStartFailedEvent;
import com.sequenceiq.datalake.flow.start.event.SdxStartSuccessEvent;
import com.sequenceiq.datalake.flow.start.event.SdxStartWaitRequest;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.start.SdxStartService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;

import reactor.bus.Event;

@Component
public class SdxStartWaitHandler extends ExceptionCatcherEventHandler<SdxStartWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxStartWaitHandler.class);

    @Value("${sdx.stack.start.sleeptime_sec:20}")
    private int sleepTimeInSec;

    @Value("${sdx.stack.start.duration_min:120}")
    private int durationInMinutes;

    @Inject
    private SdxStartService sdxStartService;

    @Override
    public String selector() {
        return "SdxStartWaitRequest";
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<SdxStartWaitRequest> event) {
        return new SdxStartFailedEvent(resourceId, null, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent event) {
        SdxStartWaitRequest waitRequest = event.getData();
        Long sdxId = waitRequest.getResourceId();
        String userId = waitRequest.getUserId();
        Selectable response;
        try {
            LOGGER.debug("Start polling stack start process for id: {}", sdxId);
            PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes, TimeUnit.MINUTES);
            sdxStartService.waitCloudbreakCluster(sdxId, pollingConfig);
            response = new SdxStartSuccessEvent(sdxId, userId);
        } catch (UserBreakException userBreakException) {
            LOGGER.error("Start polling exited before timeout. Cause: ", userBreakException);
            response = new SdxStartFailedEvent(sdxId, userId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.error("Start poller stopped for stack: {}", sdxId);
            response = new SdxStartFailedEvent(sdxId, userId,
                    new PollerStoppedException("Datalake start timed out after " + durationInMinutes + " minutes"));
        } catch (PollerException exception) {
            LOGGER.error("Start polling failed for stack: {}", sdxId);
            response = new SdxStartFailedEvent(sdxId, userId, exception);
        }
        return response;
    }
}
