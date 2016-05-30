package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.reactor.ClusterEventHandler;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartResult;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterConnector;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ClusterStartHandler implements ClusterEventHandler<ClusterStartRequest> {
    @Inject
    private AmbariClusterConnector ambariClusterConnector;
    @Inject
    private StackService stackService;
    @Inject
    private EventBus eventBus;

    @Override
    public Class<ClusterStartRequest> type() {
        return ClusterStartRequest.class;
    }

    @Override
    public void accept(Event<ClusterStartRequest> event) {
        ClusterStartRequest request = event.getData();
        ClusterStartResult result;
        try {
            Stack stack = stackService.getById(request.getStackId());
            ambariClusterConnector.startCluster(stack);
            result = new ClusterStartResult(request);
        } catch (Exception e) {
            result = new ClusterStartResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event(event.getHeaders(), result));
    }
}
