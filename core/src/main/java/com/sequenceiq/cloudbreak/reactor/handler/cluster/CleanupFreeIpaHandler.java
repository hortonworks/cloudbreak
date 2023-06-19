package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLEANUP_FREEIPA_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CLEANUP_FREEIPA_FINISHED_EVENT;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.CleanupFreeIpaEvent;
import com.sequenceiq.cloudbreak.service.freeipa.FreeIpaCleanupService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

@Component
public class CleanupFreeIpaHandler implements EventHandler<CleanupFreeIpaEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CleanupFreeIpaHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private FreeIpaCleanupService freeIpaCleanupService;

    @Inject
    private StackDtoService stackDtoService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(CleanupFreeIpaEvent.class);
    }

    @Override
    public void accept(Event<CleanupFreeIpaEvent> cleanupFreeIpaEvent) {
        CleanupFreeIpaEvent event = cleanupFreeIpaEvent.getData();
        try {
            LOGGER.debug("Handle cleanup request for hosts: {} and IPs: {}", event.getHostNames(), event.getIps());
            StackView stack = stackDtoService.getStackViewById(event.getResourceId());
            if (event.isRecover()) {
                LOGGER.debug("Invoke cleanup on recover");
                freeIpaCleanupService.cleanupOnRecover(stack, event.getHostNames(), event.getIps());
            } else {
                LOGGER.debug("Invoke cleanup on scale");
                freeIpaCleanupService.cleanupOnScale(stack, event.getHostNames(), event.getIps());
            }
            LOGGER.debug("Cleanup finished for hosts: {} and IPs: {}", event.getHostNames(), event.getIps());
            CleanupFreeIpaEvent response = new CleanupFreeIpaEvent(CLEANUP_FREEIPA_FINISHED_EVENT.event(), event.getResourceId(), event.getHostNames(),
                    event.getIps(), event.isRecover());
            Event<StackEvent> responseEvent = new Event<>(cleanupFreeIpaEvent.getHeaders(), response);
            eventBus.notify(CLEANUP_FREEIPA_FINISHED_EVENT.event(), responseEvent);
        } catch (Exception e) {
            LOGGER.error("FreeIPA cleanup failed for hosts {} and IPs: {}", event.getHostNames(), event.getIps(), e);
            StackFailureEvent response = new StackFailureEvent(CLEANUP_FREEIPA_FAILED_EVENT.event(), event.getResourceId(), e);
            Event<StackEvent> responseEvent = new Event<>(cleanupFreeIpaEvent.getHeaders(), response);
            eventBus.notify(CLEANUP_FREEIPA_FAILED_EVENT.event(), responseEvent);
        }
    }
}
