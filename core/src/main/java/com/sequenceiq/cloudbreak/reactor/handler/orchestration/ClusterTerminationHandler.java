package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterTerminationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterTerminationResult;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

/**
 * @deprecated Kept for backward compatibility
 */
@Deprecated
@Component
public class ClusterTerminationHandler implements EventHandler<ClusterTerminationRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTerminationHandler.class);

    @Inject
    private EventBus eventBus;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterTerminationRequest.class);
    }

    @Override
    public void accept(Event<ClusterTerminationRequest> event) {
        ClusterTerminationRequest request = event.getData();
        ClusterTerminationResult result;
        try {
            result = new ClusterTerminationResult(request);
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to delete cluster containers", e);
            result = new ClusterTerminationResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));

    }
}
