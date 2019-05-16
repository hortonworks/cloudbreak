package com.sequenceiq.cloudbreak.reactor;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.BootstrapNewNodesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.BootstrapNewNodesResult;
import com.sequenceiq.flow.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class BootstrapNewNodesHandler implements EventHandler<BootstrapNewNodesRequest> {
    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterBootstrapper clusterBootstrapper;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(BootstrapNewNodesRequest.class);
    }

    @Override
    public void accept(Event<BootstrapNewNodesRequest> event) {
        BootstrapNewNodesRequest request = event.getData();
        BootstrapNewNodesResult result;
        try {
            clusterBootstrapper.bootstrapNewNodes(request.getResourceId(), request.getUpscaleCandidateAddresses(), request.getHostNames());
            result = new BootstrapNewNodesResult(request);
        } catch (Exception e) {
            result = new BootstrapNewNodesResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
