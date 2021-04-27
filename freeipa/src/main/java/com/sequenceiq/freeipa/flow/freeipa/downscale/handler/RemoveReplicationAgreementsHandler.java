package com.sequenceiq.freeipa.flow.freeipa.downscale.handler;

import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.REMOVE_REPLICATION_AGREEMENTS_FAILED_EVENT;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.removereplication.RemoveReplicationAgreementsRequest;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.removereplication.RemoveReplicationAgreementsResponse;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaTopologyService;
import com.sequenceiq.freeipa.service.stack.StackService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class RemoveReplicationAgreementsHandler extends ExceptionCatcherEventHandler<RemoveReplicationAgreementsRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoveReplicationAgreementsHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private FreeIpaTopologyService freeIpaTopologyService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RemoveReplicationAgreementsRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<RemoveReplicationAgreementsRequest> event) {
        return new DownscaleFailureEvent(REMOVE_REPLICATION_AGREEMENTS_FAILED_EVENT.event(),
                resourceId, "Downscale Remove Replication Agreements", Set.of(), Map.of(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<RemoveReplicationAgreementsRequest> event) {
        RemoveReplicationAgreementsRequest request = event.getData();
        Selectable result;
        try {
            Long stackId = request.getResourceId();
            Stack stack = stackService.getStackById(stackId);
            FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStack(stack);
            freeIpaTopologyService.updateReplicationTopology(stackId, request.getHosts(), freeIpaClient);

            result = new RemoveReplicationAgreementsResponse(request.getResourceId());
        } catch (Exception e) {
            LOGGER.error("Downscale removing replication agreements failed", e);
            result = new DownscaleFailureEvent(REMOVE_REPLICATION_AGREEMENTS_FAILED_EVENT.event(),
                    request.getResourceId(), "Downscale Remove Replication Agreements", Set.of(), Map.of(), e);
        }
        return result;
    }
}
