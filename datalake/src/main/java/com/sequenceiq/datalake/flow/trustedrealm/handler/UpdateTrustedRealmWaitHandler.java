package com.sequenceiq.datalake.flow.trustedrealm.handler;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.flow.trustedrealm.event.UpdateTrustedRealmFailureResponse;
import com.sequenceiq.datalake.flow.trustedrealm.event.UpdateTrustedRealmSuccessResponse;
import com.sequenceiq.datalake.flow.trustedrealm.event.UpdateTrustedRealmWaitRequest;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.flowwait.SdxWaitService;
import com.sequenceiq.datalake.service.sdx.flowwait.exception.SdxWaitException;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class UpdateTrustedRealmWaitHandler extends ExceptionCatcherEventHandler<UpdateTrustedRealmWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateTrustedRealmWaitHandler.class);

    @Value("${sdx.stack.updatetrustedrealm.sleeptime_sec:10}")
    private long sleepTimeInSec;

    @Value("${sdx.stack.updatetrustedrealm.duration_min:30}")
    private long durationInMinutes;

    @Inject
    private SdxWaitService sdxWaitService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpdateTrustedRealmWaitRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpdateTrustedRealmWaitRequest> event) {
        LOGGER.warn("Fallback to default failure event for exception", e);
        return new UpdateTrustedRealmFailureResponse(resourceId, event.getData().getUserId(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<UpdateTrustedRealmWaitRequest> event) {
        UpdateTrustedRealmWaitRequest eventData = event.getData();
        try {
            PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes, TimeUnit.MINUTES)
                    .withStopPollingIfExceptionOccurred(true);
            sdxWaitService.waitForCloudbreakFlow(eventData.getResourceId(), pollingConfig, "Updating trusted realm");
            return new UpdateTrustedRealmSuccessResponse(eventData.getResourceId(), eventData.getUserId());
        } catch (SdxWaitException e) {
            LOGGER.warn("Error occurred during waiting for update trusted realm for SDX stack {}, error: ",
                    eventData.getResourceId(), e);
            return new UpdateTrustedRealmFailureResponse(eventData.getResourceId(), eventData.getUserId(), e);
        }
    }
}
