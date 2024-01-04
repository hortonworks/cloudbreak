package com.sequenceiq.cloudbreak.reactor;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.cluster.AmbariClusterResetService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterResetRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterResetResult;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

@Component
public class ClusterResetHandler implements EventHandler<ClusterResetRequest> {
    @Inject
    private EventBus eventBus;

    @Inject
    private AmbariClusterResetService ambariClusterResetService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterResetRequest.class);
    }

    @Override
    public void accept(Event<ClusterResetRequest> event) {
        ClusterResetRequest request = event.getData();
        ClusterResetResult result;
        try {
            ambariClusterResetService.resetCluster(request.getResourceId());
            result = new ClusterResetResult(request);
        } catch (Exception e) {
            result = new ClusterResetResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }

}
