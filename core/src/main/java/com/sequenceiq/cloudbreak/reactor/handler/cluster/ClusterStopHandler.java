package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStopRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStopResult;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ClusterStopHandler implements ReactorEventHandler<ClusterStopRequest> {
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
            Stack stack = stackService.getByIdWithListsInTransaction(request.getStackId());
            apiConnectors.getConnector(stack).stopCluster();
            result = new ClusterStopResult(request);
        } catch (Exception e) {
            result = new ClusterStopResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
