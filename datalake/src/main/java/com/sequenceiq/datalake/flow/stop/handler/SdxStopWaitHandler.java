package com.sequenceiq.datalake.flow.stop.handler;

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
import com.sequenceiq.datalake.flow.stop.event.SdxStopFailedEvent;
import com.sequenceiq.datalake.flow.stop.event.SdxStopSuccessEvent;
import com.sequenceiq.datalake.flow.stop.event.SdxStopWaitRequest;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.stop.SdxStopService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;

import reactor.bus.Event;

@Component
public class SdxStopWaitHandler extends ExceptionCatcherEventHandler<SdxStopWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxStopWaitHandler.class);

    @Value("${sdx.stack.stop.sleeptime_sec:20}")
    private int sleepTimeInSec;

    @Value("${sdx.stack.stop.duration_min:40}")
    private int durationInMinutes;

    @Inject
    private SdxStopService sdxStopService;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<SdxStopWaitRequest> event) {
        return new SdxStopFailedEvent(resourceId, null, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent event) {
        SdxStopWaitRequest waitRequest = event.getData();
        Long sdxId = waitRequest.getResourceId();
        String userId = waitRequest.getUserId();
        Selectable response;
        try {
            LOGGER.debug("Stop polling stack stopping process for id: {}", sdxId);
            PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes, TimeUnit.MINUTES);
            sdxStopService.waitCloudbreakCluster(sdxId, pollingConfig);
            response = new SdxStopSuccessEvent(sdxId, userId);
        } catch (UserBreakException userBreakException) {
            LOGGER.error("Stop polling exited before timeout. Cause: ", userBreakException);
            response = new SdxStopFailedEvent(sdxId, userId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.error("Stop poller stopped for stack: {}", sdxId);
            response = new SdxStopFailedEvent(sdxId, userId,
                    new PollerStoppedException("Datalake stop timed out after " + durationInMinutes + " minutes"));
        } catch (PollerException exception) {
            LOGGER.error("Stop polling failed for stack: {}", sdxId);
            response = new SdxStopFailedEvent(sdxId, userId, exception);
        }
        return response;
    }

    @Override
    public String selector() {
        return "SdxStopWaitRequest";
    }
}
