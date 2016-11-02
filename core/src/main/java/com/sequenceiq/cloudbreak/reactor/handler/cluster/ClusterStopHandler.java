package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.reactor.ClusterEventHandler;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStopRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStopResult;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterConnector;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ClusterStopHandler implements ClusterEventHandler<ClusterStopRequest> {
    @Inject
    private AmbariClusterConnector ambariClusterConnector;

    @Inject
    private StackService stackService;

    @Inject
    private EventBus eventBus;

    @Override
    public Class<ClusterStopRequest> type() {
        return ClusterStopRequest.class;
    }

    @Override
    public void accept(Event<ClusterStopRequest> event) {
        ClusterStopRequest request = event.getData();
        ClusterStopResult result;
        try {
            Stack stack = stackService.getById(request.getStackId());
            ambariClusterConnector.stopCluster(stack);
            result = new ClusterStopResult(request);
        } catch (Exception e) {
            result = new ClusterStopResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event(event.getHeaders(), result));
    }
}
