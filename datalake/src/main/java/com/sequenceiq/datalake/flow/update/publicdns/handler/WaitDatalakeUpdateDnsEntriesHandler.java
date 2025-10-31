package com.sequenceiq.datalake.flow.update.publicdns.handler;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.update.publicdns.DatalakeUpdatePublicDnsEntriesFailedEvent;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.flowwait.SdxWaitService;
import com.sequenceiq.datalake.service.sdx.flowwait.exception.SdxWaitException;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class WaitDatalakeUpdateDnsEntriesHandler extends ExceptionCatcherEventHandler<WaitDatalakeUpdateDnsEntriesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaitDatalakeUpdateDnsEntriesHandler.class);

    @Value("${sdx.stack.update-public-dns-entries.sleeptime-sec}")
    private int sleepTimeInSec;

    @Value("${sdx.stack.update-public-dns-entries.duration-min}")
    private int durationInMinutes;

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxWaitService sdxWaitService;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<WaitDatalakeUpdateDnsEntriesRequest> event) {
        return new DatalakeUpdatePublicDnsEntriesFailedEvent(resourceId, event.getData().getUserId(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<WaitDatalakeUpdateDnsEntriesRequest> event) {
        LOGGER.info("Start polling update of public DNS entries for datalake, event: {}", event);
        WaitDatalakeUpdateDnsEntriesRequest request = event.getData();
        Long sdxId = request.getResourceId();
        String userId = request.getUserId();
        try {
            SdxCluster sdxCluster = sdxService.getById(sdxId);
            PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes, TimeUnit.MINUTES)
                    .withStopPollingIfExceptionOccurred(true);
            sdxWaitService.waitForCloudbreakFlow(sdxCluster, pollingConfig, "Updating public DNS entries");
            LOGGER.info("Update of public DNS entries finished for SDX stack {}.", sdxId);
            return new WaitDatalakeUpdateDnsEntriesResponse(sdxId, userId);
        } catch (SdxWaitException e) {
            LOGGER.warn("Update of public DNS entries failed for SDX stack {}", sdxId, e);
            return new DatalakeUpdatePublicDnsEntriesFailedEvent(sdxId, userId, e);
        }
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(WaitDatalakeUpdateDnsEntriesRequest.class);
    }
}
