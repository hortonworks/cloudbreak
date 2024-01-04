package com.sequenceiq.datalake.flow.modifyproxy.handler;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.flow.modifyproxy.event.ModifyProxyConfigFailureResponse;
import com.sequenceiq.datalake.flow.modifyproxy.event.ModifyProxyConfigSuccessResponse;
import com.sequenceiq.datalake.flow.modifyproxy.event.ModifyProxyConfigWaitRequest;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.flowwait.SdxWaitService;
import com.sequenceiq.datalake.service.sdx.flowwait.exception.SdxWaitException;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ModifyProxyConfigWaitHandler extends ExceptionCatcherEventHandler<ModifyProxyConfigWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModifyProxyConfigWaitHandler.class);

    @Value("${sdx.stack.modifyproxy.sleeptime_sec}")
    private long sleepTimeInSec;

    @Value("${sdx.stack.modifyproxy.duration_min}")
    private long durationInMinutes;

    @Inject
    private SdxWaitService sdxWaitService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ModifyProxyConfigWaitRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ModifyProxyConfigWaitRequest> event) {
        LOGGER.warn("Fallback to default failure event for exception", e);
        return new ModifyProxyConfigFailureResponse(resourceId, null, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ModifyProxyConfigWaitRequest> event) {
        ModifyProxyConfigWaitRequest eventData = event.getData();
        try {
            PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes, TimeUnit.MINUTES)
                    .withStopPollingIfExceptionOccurred(true);
            sdxWaitService.waitForCloudbreakFlow(eventData.getResourceId(), pollingConfig, "Modifying proxy config");
            return new ModifyProxyConfigSuccessResponse(eventData.getResourceId(), event.getData().getUserId());
        } catch (SdxWaitException e) {
            LOGGER.warn("Error occurred during waiting for modify proxy config {}, error: ",
                    eventData.getResourceId(), e);
            return new ModifyProxyConfigFailureResponse(eventData.getResourceId(), eventData.getUserId(), e);
        }
    }
}
