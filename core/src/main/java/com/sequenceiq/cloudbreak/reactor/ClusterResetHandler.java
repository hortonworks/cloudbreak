package com.sequenceiq.cloudbreak.reactor;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.cluster.AmbariClusterResetService;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterResetRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterResetResult;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ClusterResetHandler implements ClusterEventHandler<ClusterResetRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterResetHandler.class);
    @Inject
    private EventBus eventBus;
    @Inject
    private StackService stackService;
    @Inject
    private AmbariClusterResetService ambariClusterResetService;

    @Override
    public Class<ClusterResetRequest> type() {
        return ClusterResetRequest.class;
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
        eventBus.notify(result.selector(), new Event(event.getHeaders(), result));
    }

}
