package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.cluster.ClusterUpscaleService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterManagerStopComponentsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StopClusterComponentsResult;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class AmbariStopComponentsHandler implements EventHandler<ClusterManagerStopComponentsRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariStopComponentsHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterUpscaleService clusterUpscaleService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterManagerStopComponentsRequest.class);
    }

    @Override
    public void accept(Event<ClusterManagerStopComponentsRequest> event) {
        ClusterManagerStopComponentsRequest request = event.getData();
        Long stackId = request.getResourceId();
        StopClusterComponentsResult result;
        try {
            clusterUpscaleService.stopComponents(stackId, request.getComponents(), request.getHostName());
            result = new StopClusterComponentsResult(request);
        } catch (Exception e) {
            String message = "Failed to install components on new host";
            LOGGER.error(message, e);
            result = new StopClusterComponentsResult(message, e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));

    }

}
