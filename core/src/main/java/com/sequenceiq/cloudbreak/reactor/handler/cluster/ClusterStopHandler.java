package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStopRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStopResult;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ClusterStopHandler implements EventHandler<ClusterStopRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterStopHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterApiConnectors apiConnectors;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterStopRequest.class);
    }

    @Override
    public void accept(Event<ClusterStopRequest> event) {
        ClusterStopRequest request = event.getData();
        ClusterStopResult result;
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(request.getResourceId());
            boolean clusterManagerRunning = apiConnectors.getConnector(stack).clusterStatusService().isClusterManagerRunning();
            if (clusterManagerRunning) {
                apiConnectors.getConnector(stack).stopCluster(false);
            } else {
                LOGGER.info("Cluster manager isn't running, cannot stop it.");
            }
            result = new ClusterStopResult(request);
        } catch (Exception e) {
            LOGGER.warn("Cannot stop the cluster: ", e);
            result = new ClusterStopResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
