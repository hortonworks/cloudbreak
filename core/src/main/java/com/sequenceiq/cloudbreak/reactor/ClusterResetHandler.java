package com.sequenceiq.cloudbreak.reactor;

import com.sequenceiq.cloudbreak.core.cluster.AmbariClusterResetService;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterResetRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterResetResult;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import org.springframework.stereotype.Component;
import reactor.bus.Event;
import reactor.bus.EventBus;

import javax.inject.Inject;

@Component
public class ClusterResetHandler implements ReactorEventHandler<ClusterResetRequest> {
    @Inject
    private EventBus eventBus;

    @Inject
    private StackService stackService;

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
            ambariClusterResetService.resetCluster(request.getStackId());
            result = new ClusterResetResult(request);
        } catch (Exception e) {
            result = new ClusterResetResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }

}
