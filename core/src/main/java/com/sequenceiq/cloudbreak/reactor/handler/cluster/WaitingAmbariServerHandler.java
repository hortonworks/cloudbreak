package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.WaitForAmbariServerFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.WaitForAmbariServerRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.WaitForAmbariServerSuccess;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariClusterConnector;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class WaitingAmbariServerHandler implements ReactorEventHandler<WaitForAmbariServerRequest> {
    @Inject
    private StackService stackService;

    @Inject
    private AmbariClusterConnector ambariClusterConnector;

    @Inject
    private EventBus eventBus;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(WaitForAmbariServerRequest.class);
    }

    @Override
    public void accept(Event<WaitForAmbariServerRequest> event) {
        Long stackId = event.getData().getStackId();
        Selectable response;
        try {
            Stack stack = stackService.getByIdWithoutAuth(stackId);
            ambariClusterConnector.waitForServer(stack);
            response = new WaitForAmbariServerSuccess(stackId);
        } catch (Exception e) {
            response = new WaitForAmbariServerFailed(stackId, e);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
