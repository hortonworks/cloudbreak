package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLEANUP_FREEIPA_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLEANUP_FREEIPA_FINISHED_EVENT;

import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.CleanupAdEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.CleanupFreeIpaEvent;
import com.sequenceiq.cloudbreak.service.ad.AdCleanupService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class CleanupAdHandler extends ExceptionCatcherEventHandler<CleanupAdEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CleanupAdHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private AdCleanupService adCleanupService;

    @Inject
    private StackDtoService stackDtoService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(CleanupAdEvent.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<CleanupAdEvent> event) {
        return new StackFailureEvent(CLEANUP_FREEIPA_FAILED_EVENT.event(), resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<CleanupAdEvent> event) {
        Selectable response;
        try {
            CleanupAdEvent cleanupAdEvent = event.getData();
            LOGGER.debug("Handle AD cleanup request: {}", cleanupAdEvent);
            Set<String> hostnames = cleanupAdEvent.getHostNames();
            StackDto stack = stackDtoService.getById(cleanupAdEvent.getResourceId());
            adCleanupService.cleanUpAd(hostnames, stack);

            response = new CleanupFreeIpaEvent(CLEANUP_FREEIPA_FINISHED_EVENT.event(), cleanupAdEvent.getResourceId(),
                    cleanupAdEvent.getHostNames(), cleanupAdEvent.getIps(), false);
        } catch (CloudbreakOrchestratorFailedException e) {
            LOGGER.error("AD cleanup failed", e);
            response = new StackFailureEvent(CLEANUP_FREEIPA_FAILED_EVENT.event(), event.getData().getResourceId(), e);
        }
        return response;
    }
}