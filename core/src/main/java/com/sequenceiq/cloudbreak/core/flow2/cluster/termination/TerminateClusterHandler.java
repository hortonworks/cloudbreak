package com.sequenceiq.cloudbreak.core.flow2.cluster.termination;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.core.flow.handlers.AmbariClusterEventHandler;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterTerminationService;
import com.sequenceiq.cloudbreak.service.stack.flow.TerminationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class TerminateClusterHandler implements AmbariClusterEventHandler<TerminateClusterRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TerminateClusterHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterTerminationService clusterTerminationService;

    @Override
    public void accept(Event<TerminateClusterRequest> terminateClusterRequestEvent) {
        LOGGER.info("Received event: {}", terminateClusterRequestEvent);
        TerminateClusterRequest request = terminateClusterRequestEvent.getData();
        try {
            clusterTerminationService.deleteClusterContainers(request.getClusterContext().getClusterId());
        } catch (TerminationFailedException e) {
            LOGGER.error("Failed to delete cluster containers: {}", e);
            TerminateClusterResult result = new TerminateClusterResult("Cluster termination failed.", e, request);
            eventBus.notify(result.selector(), new Event(terminateClusterRequestEvent.getHeaders(), result));
        }

        TerminateClusterResult result = new TerminateClusterResult(request);
        eventBus.notify(result.selector(), new Event(terminateClusterRequestEvent.getHeaders(), result));

    }

    @Override
    public Class<TerminateClusterRequest> type() {
        return TerminateClusterRequest.class;
    }
}
