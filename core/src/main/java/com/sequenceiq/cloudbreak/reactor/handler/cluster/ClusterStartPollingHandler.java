package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartPollingRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartPollingResult;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

@Component
public class ClusterStartPollingHandler implements EventHandler<ClusterStartPollingRequest> {

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private StackService stackService;

    @Inject
    private EventBus eventBus;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterStartPollingRequest.class);
    }

    @Override
    public void accept(Event<ClusterStartPollingRequest> event) {
        ClusterStartPollingRequest request = event.getData();
        ClusterStartPollingResult result;
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(request.getResourceId());
            ClusterApi connector = clusterApiConnectors.getConnector(stack);
            connector.waitForServices(request.getRequestId());
            result = new ClusterStartPollingResult(request);
        } catch (Exception e) {
            result = new ClusterStartPollingResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
