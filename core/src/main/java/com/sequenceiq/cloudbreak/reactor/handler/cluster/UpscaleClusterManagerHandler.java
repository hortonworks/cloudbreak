package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.cluster.ClusterManagerUpscaleService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.UpscaleClusterManagerRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.UpscaleClusterManagerResult;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

@Component
public class UpscaleClusterManagerHandler implements EventHandler<UpscaleClusterManagerRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpscaleClusterManagerHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterManagerUpscaleService clusterManagerUpscaleService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpscaleClusterManagerRequest.class);
    }

    @Override
    public void accept(Event<UpscaleClusterManagerRequest> event) {
        UpscaleClusterManagerRequest request = event.getData();
        UpscaleClusterManagerResult result;
        try {
            clusterManagerUpscaleService.upscaleClusterManager(request.getResourceId(), request.getHostGroupWithAdjustment(),
                    request.isPrimaryGatewayChanged(), request.isRepair());
            result = new UpscaleClusterManagerResult(request);
        } catch (Exception e) {
            LOGGER.warn("Upscale Cluster Manager failed due to {}.", e.getMessage(), e);
            result = new UpscaleClusterManagerResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
