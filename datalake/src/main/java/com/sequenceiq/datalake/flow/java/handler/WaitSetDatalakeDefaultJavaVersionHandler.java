package com.sequenceiq.datalake.flow.java.handler;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.java.SetDatalakeDefaultJavaVersionFailedEvent;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.flowwait.SdxWaitService;
import com.sequenceiq.datalake.service.sdx.flowwait.exception.SdxWaitException;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class WaitSetDatalakeDefaultJavaVersionHandler extends ExceptionCatcherEventHandler<WaitSetDatalakeDefaultJavaVersionRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaitSetDatalakeDefaultJavaVersionHandler.class);

    @Value("${sdx.stack.set-default-java-version.sleeptime-sec}")
    private int sleepTimeInSec;

    @Value("${sdx.stack.set-default-java-version.duration-min}")
    private int durationInMinutes;

    @Inject
    private SdxWaitService sdxWaitService;

    @Inject
    private SdxService sdxService;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<WaitSetDatalakeDefaultJavaVersionRequest> event) {
        return new SetDatalakeDefaultJavaVersionFailedEvent(resourceId, event.getData().getUserId(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<WaitSetDatalakeDefaultJavaVersionRequest> event) {
        LOGGER.info("Start polling for setting default java version, event: {}", event);
        WaitSetDatalakeDefaultJavaVersionRequest request = event.getData();
        Long sdxId = request.getResourceId();
        String userId = request.getUserId();
        try {
            SdxCluster sdxCluster = sdxService.getById(sdxId);
            PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes, TimeUnit.MINUTES)
                    .withStopPollingIfExceptionOccurred(true);
            sdxWaitService.waitForCloudbreakFlow(sdxCluster, pollingConfig, "Setting default java version");
            LOGGER.info("Setting default java version finished for SDX stack {}", sdxId);
            return new WaitSetDatalakeDefaultJavaVersionResult(sdxId, userId);
        } catch (SdxWaitException e) {
            LOGGER.warn("Setting default java version failed for SDX stack {}", sdxId, e);
            return new SetDatalakeDefaultJavaVersionFailedEvent(sdxId, userId, e);
        }
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(WaitSetDatalakeDefaultJavaVersionRequest.class);
    }
}
