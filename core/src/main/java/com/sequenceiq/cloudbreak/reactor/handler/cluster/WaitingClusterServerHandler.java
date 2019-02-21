package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.WaitForAmbariServerFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.WaitForClusterServerRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.WaitForAmbariServerSuccess;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class WaitingClusterServerHandler implements ReactorEventHandler<WaitForClusterServerRequest> {
    @Inject
    private StackService stackService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private EventBus eventBus;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(WaitForClusterServerRequest.class);
    }

    @Override
    public void accept(Event<WaitForClusterServerRequest> event) {
        Long stackId = event.getData().getStackId();
        Selectable response;
        try {
            Stack stack = stackService.getByIdWithTransaction(stackId);
            clusterApiConnectors.getConnector(stack).waitForServer(stack);
            response = new WaitForAmbariServerSuccess(stackId);
        } catch (Exception e) {
            response = new WaitForAmbariServerFailed(stackId, e);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
