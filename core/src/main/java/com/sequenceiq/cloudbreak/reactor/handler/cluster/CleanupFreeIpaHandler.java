package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLEANUP_FREEIPA_FINISHED_EVENT;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.CleanupFreeIpaEvent;
import com.sequenceiq.cloudbreak.service.freeipa.FreeIpaCleanupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class CleanupFreeIpaHandler implements EventHandler<CleanupFreeIpaEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CleanupFreeIpaHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private FreeIpaCleanupService freeIpaCleanupService;

    @Inject
    private StackService stackService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(CleanupFreeIpaEvent.class);
    }

    @Override
    public void accept(Event<CleanupFreeIpaEvent> cleanupFreeIpaEvent) {
        CleanupFreeIpaEvent event = cleanupFreeIpaEvent.getData();
        try {
            LOGGER.debug("Handle cleanup request for hosts: {} and IPs: {}", event.getHostNames(), event.getIps());
            Stack stack = stackService.get(event.getResourceId());
            freeIpaCleanupService.cleanup(stack, true, event.isRecover(), event.getHostNames(), event.getIps());
            LOGGER.debug("Cleanup finished for hosts: {} and IPs: {}", event.getHostNames(), event.getIps());
        } catch (Exception e) {
            LOGGER.error("FreeIPA cleanup failed for hosts {} and IPs: {}", event.getHostNames(), event.getIps(), e);
        } finally {
            CleanupFreeIpaEvent response = new CleanupFreeIpaEvent(CLEANUP_FREEIPA_FINISHED_EVENT.event(), event.getResourceId(), event.getHostNames(),
                    event.getIps(), event.isRecover());
            Event<StackEvent> responseEvent = new Event<>(cleanupFreeIpaEvent.getHeaders(), response);
            eventBus.notify(CLEANUP_FREEIPA_FINISHED_EVENT.event(), responseEvent);
        }
    }
}
