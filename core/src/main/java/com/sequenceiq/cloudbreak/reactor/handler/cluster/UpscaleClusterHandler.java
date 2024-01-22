package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.cluster.ClusterUpscaleService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.UpscaleClusterRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.UpscaleClusterResult;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

@Component
public class UpscaleClusterHandler implements EventHandler<UpscaleClusterRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpscaleClusterHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterUpscaleService clusterUpscaleService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpscaleClusterRequest.class);
    }

    @Override
    public void accept(Event<UpscaleClusterRequest> event) {
        LOGGER.debug("Accepting cluster upscale event: {}", event);
        UpscaleClusterRequest request = event.getData();
        UpscaleClusterResult result;
        try {
            clusterUpscaleService.installServicesOnNewHosts(request);
            result = new UpscaleClusterResult(request);
        } catch (Exception e) {
            LOGGER.debug("Failed to upscale cluster", e);
            result = new UpscaleClusterResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
