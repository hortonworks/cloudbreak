package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterTerminationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterTerminationResult;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterTerminationService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ClusterTerminationHandler implements ReactorEventHandler<ClusterTerminationRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTerminationHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterTerminationService clusterTerminationService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterTerminationRequest.class);
    }

    @Override
    public void accept(Event<ClusterTerminationRequest> event) {
        ClusterTerminationRequest request = event.getData();
        ClusterTerminationResult result;
        try {
            Boolean allowed = clusterTerminationService.deleteClusterContainers(request.getClusterId());
            result = new ClusterTerminationResult(request, allowed);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to delete cluster containers: {}", e);
            result = new ClusterTerminationResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event(event.getHeaders(), result));

    }
}
