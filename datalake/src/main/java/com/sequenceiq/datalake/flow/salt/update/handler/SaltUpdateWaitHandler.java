package com.sequenceiq.datalake.flow.salt.update.handler;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.salt.update.event.SaltUpdateFailureResponse;
import com.sequenceiq.datalake.flow.salt.update.event.SaltUpdateWaitRequest;
import com.sequenceiq.datalake.flow.salt.update.event.SaltUpdateWaitSuccessResponse;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.flowwait.SdxWaitService;
import com.sequenceiq.datalake.service.sdx.flowwait.exception.SdxWaitException;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class SaltUpdateWaitHandler extends ExceptionCatcherEventHandler<SaltUpdateWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltUpdateWaitHandler.class);

    @Value("${sdx.stack.saltupdate.sleeptime-sec}")
    private int sleepTimeInSec;

    @Value("${sdx.stack.saltupdate.duration-min}")
    private int durationInMinutes;

    @Inject
    private SdxWaitService sdxWaitService;

    @Inject
    private SdxService sdxService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(SaltUpdateWaitRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<SaltUpdateWaitRequest> event) {
        LOGGER.warn("Exception during polling Salt update: ", e);
        return new SaltUpdateFailureResponse(resourceId, null, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<SaltUpdateWaitRequest> event) {
        LOGGER.debug("Start polling for Salt update, event: {}", event);
        SaltUpdateWaitRequest request = event.getData();
        Long sdxId = request.getResourceId();
        String userId = request.getUserId();
        try {
            SdxCluster sdxCluster = sdxService.getById(sdxId);
            PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes, TimeUnit.MINUTES)
                    .withStopPollingIfExceptionOccurred(true);
            sdxWaitService.waitForCloudbreakFlow(sdxCluster, pollingConfig, "Running Salt update");
            LOGGER.debug("Salt update finished successfully in core.");
            return new SaltUpdateWaitSuccessResponse(sdxId, userId);
        } catch (SdxWaitException e) {
            LOGGER.warn("Error occurred during waiting for salt update for SDX stack {}, error: ", sdxId, e);
            return new SaltUpdateFailureResponse(sdxId, userId, e);
        }
    }
}
