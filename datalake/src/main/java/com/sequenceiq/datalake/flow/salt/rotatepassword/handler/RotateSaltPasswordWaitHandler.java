package com.sequenceiq.datalake.flow.salt.rotatepassword.handler;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.flow.salt.rotatepassword.event.RotateSaltPasswordFailureResponse;
import com.sequenceiq.datalake.flow.salt.rotatepassword.event.RotateSaltPasswordSuccessResponse;
import com.sequenceiq.datalake.flow.salt.rotatepassword.event.RotateSaltPasswordWaitRequest;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.flowwait.SdxWaitService;
import com.sequenceiq.datalake.service.sdx.flowwait.exception.SdxWaitException;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class RotateSaltPasswordWaitHandler extends ExceptionCatcherEventHandler<RotateSaltPasswordWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RotateSaltPasswordWaitHandler.class);

    @Value("${sdx.stack.rotatesaltpassword.sleeptime_sec}")
    private long sleepTimeInSec;

    @Value("${sdx.stack.rotatesaltpassword.duration_min}")
    private long durationInMinutes;

    @Inject
    private SdxWaitService sdxWaitService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RotateSaltPasswordWaitRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<RotateSaltPasswordWaitRequest> event) {
        LOGGER.warn("Fallback to default failure event for exception", e);
        return new RotateSaltPasswordFailureResponse(resourceId, null, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<RotateSaltPasswordWaitRequest> event) {
        RotateSaltPasswordWaitRequest eventData = event.getData();
        try {
            PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes, TimeUnit.MINUTES)
                    .withStopPollingIfExceptionOccurred(true);
            sdxWaitService.waitForCloudbreakFlow(eventData.getResourceId(), pollingConfig, "Rotating SaltStack user password");
            return new RotateSaltPasswordSuccessResponse(eventData.getResourceId(), event.getData().getUserId());
        } catch (SdxWaitException e) {
            LOGGER.warn("Error occurred during waiting for salt password rotation {}, error: ",
                    eventData.getResourceId(), e);
            return new RotateSaltPasswordFailureResponse(eventData.getResourceId(), eventData.getUserId(), e);
        }
    }
}
