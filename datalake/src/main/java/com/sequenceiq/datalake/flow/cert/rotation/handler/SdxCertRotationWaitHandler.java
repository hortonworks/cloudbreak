package com.sequenceiq.datalake.flow.cert.rotation.handler;

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
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.cert.rotation.event.SdxCertRotationEvent;
import com.sequenceiq.datalake.flow.cert.rotation.event.SdxCertRotationFailedEvent;
import com.sequenceiq.datalake.flow.cert.rotation.event.SdxCertRotationWaitEvent;
import com.sequenceiq.datalake.flow.repair.event.SdxRepairFailedEvent;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.cert.CertRotationService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class SdxCertRotationWaitHandler extends ExceptionCatcherEventHandler<SdxCertRotationWaitEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxCertRotationWaitHandler.class);

    @Value("${sdx.stack.cert.rotation.sleeptime_sec:20}")
    private int sleepTimeInSec;

    @Value("${sdx.stack.cert.rotation.duration_min:60}")
    private int durationInMinutes;

    @Inject
    private CertRotationService certRotationService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(SdxCertRotationWaitEvent.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<SdxCertRotationWaitEvent> event) {
        return new SdxRepairFailedEvent(resourceId, event.getData().getUserId(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<SdxCertRotationWaitEvent> event) {
        SdxEvent sdxEvent = event.getData();
        Long sdxId = sdxEvent.getResourceId();
        String userId = sdxEvent.getUserId();
        Selectable response;
        try {
            LOGGER.debug("Start polling stack cert rotation process for id: {}", sdxId);
            PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes, TimeUnit.MINUTES);
            certRotationService.waitForCloudbreakClusterCertRotation(sdxId, pollingConfig);
            response = new SdxEvent(SdxCertRotationEvent.CERT_ROTATION_FINISHED_EVENT.event(), sdxId, userId);
        } catch (UserBreakException userBreakException) {
            LOGGER.error("Cert rotation polling exited before timeout. Cause: ", userBreakException);
            response = new SdxCertRotationFailedEvent(sdxId, userId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.error("Cert rotation poller stopped for stack: {}", sdxId);
            response = new SdxCertRotationFailedEvent(sdxId, userId,
                    new PollerStoppedException("Datalake cert rotation timed out after " + durationInMinutes + " minutes"));
        } catch (PollerException exception) {
            LOGGER.error("Cert rotation polling failed for stack: {}", sdxId);
            response = new SdxCertRotationFailedEvent(sdxId, userId, exception);
        }
        return response;
    }
}
