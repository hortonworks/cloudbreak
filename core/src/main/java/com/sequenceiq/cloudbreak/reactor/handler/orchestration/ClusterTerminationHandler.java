package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.reactor.ClusterEventHandler;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterTerminationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterTerminationResult;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterTerminationService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ClusterTerminationHandler implements ClusterEventHandler<ClusterTerminationRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTerminationHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterTerminationService clusterTerminationService;

    @Override
    public Class<ClusterTerminationRequest> type() {
        return ClusterTerminationRequest.class;
    }

    @Override
    public void accept(Event<ClusterTerminationRequest> event) {
        ClusterTerminationRequest request = event.getData();
        ClusterTerminationResult result;
        try {
            clusterTerminationService.deleteClusterContainers(request.getClusterId());
            result = new ClusterTerminationResult(request);
        } catch (Exception e) {
            LOGGER.error("Failed to delete cluster containers: {}", e);
            result = new ClusterTerminationResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event(event.getHeaders(), result));

    }
}
