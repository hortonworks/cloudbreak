package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartPollingRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartPollingResult;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ClusterStartPollingHandler implements ReactorEventHandler<ClusterStartPollingRequest> {

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
            Stack stack = stackService.getByIdWithListsInTransaction(request.getStackId());
            ClusterApi connector = clusterApiConnectors.getConnector(stack);
            connector.waitForServices(request.getRequestId());
            result = new ClusterStartPollingResult(request);
        } catch (Exception e) {
            result = new ClusterStartPollingResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
