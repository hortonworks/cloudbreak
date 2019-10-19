package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.cluster.ClusterUpscaleService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterManagerStartComponentsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterManagerStartComponentsResult;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class AmbariStartComponentsHandler implements EventHandler<ClusterManagerStartComponentsRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariStartComponentsHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterUpscaleService clusterUpscaleService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterManagerStartComponentsRequest.class);
    }

    @Override
    public void accept(Event<ClusterManagerStartComponentsRequest> event) {
        ClusterManagerStartComponentsRequest request = event.getData();
        Long stackId = request.getResourceId();
        ClusterManagerStartComponentsResult result;
        try {
            clusterUpscaleService.startComponents(stackId, request.getComponents(), request.getHostName());
            result = new ClusterManagerStartComponentsResult(request);
        } catch (Exception e) {
            String message = "Failed to start components on new host";
            LOGGER.error(message, e);
            result = new ClusterManagerStartComponentsResult(message, e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));

    }
}
