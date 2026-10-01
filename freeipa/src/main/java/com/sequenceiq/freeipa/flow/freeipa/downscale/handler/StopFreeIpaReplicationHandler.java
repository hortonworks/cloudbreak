package com.sequenceiq.freeipa.flow.freeipa.downscale.handler;

import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.stopreplication.StopFreeIpaReplicationRequest;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaServicesStopService;

@Component
public class StopFreeIpaReplicationHandler extends ExceptionCatcherEventHandler<StopFreeIpaReplicationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopFreeIpaReplicationHandler.class);

    @Inject
    private FreeIpaServicesStopService freeIpaServicesStopService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(StopFreeIpaReplicationRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<StopFreeIpaReplicationRequest> event) {
        return new DownscaleFailureEvent(resourceId, "Stopping FreeIPA replication", Set.of(), Map.of(), e);
    }

    @Override
    public Selectable doAccept(HandlerEvent<StopFreeIpaReplicationRequest> event) {
        StopFreeIpaReplicationRequest request = event.getData();
        LOGGER.info("Gracefully stopping FreeIPA services on departing nodes {} before termination to release replication locks on the survivors",
                request.getInstanceIds());
        try {
            freeIpaServicesStopService.stopServicesOnInstances(request.getResourceId(), request.getInstanceIds());
        } catch (Exception e) {
            LOGGER.warn("Failed to stop FreeIPA services on {}. Continuing with downscale.", request.getInstanceIds(), e);
        }
        return new StackEvent(DownscaleFlowEvent.STOP_FREEIPA_REPLICATION_FINISHED.event(), request.getResourceId());
    }
}
