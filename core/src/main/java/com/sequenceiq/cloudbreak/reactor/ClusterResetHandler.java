package com.sequenceiq.cloudbreak.reactor;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterResetRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterResetResult;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterConnector;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ClusterResetHandler implements ClusterEventHandler<ClusterResetRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterResetHandler.class);
    @Inject
    private AmbariClusterConnector ambariClusterConnector;
    @Inject
    private EventBus eventBus;

    @Override
    public Class<ClusterResetRequest> type() {
        return ClusterResetRequest.class;
    }

    @Override
    public void accept(Event<ClusterResetRequest> event) {
        ClusterResetRequest request = event.getData();
        ClusterResetResult result;
        try {
            //TODO This feature is not working because it is relying on the recipe feature.
            LOGGER.warn("Cluster reinstall feature is under construction. It's not working for now.");
            underConstruction();
            //ambariClusterConnector.resetAmbariCluster(request.getStackId());
            result = new ClusterResetResult(request);
        } catch (Exception e) {
            result = new ClusterResetResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event(event.getHeaders(), result));
    }

    private void underConstruction() {
        throw new RuntimeException("Under construction. Cluster reinstall feature is not working for now.");
    }
}
