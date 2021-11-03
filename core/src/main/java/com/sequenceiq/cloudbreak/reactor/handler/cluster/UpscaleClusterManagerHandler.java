package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.cluster.ClusterManagerUpscaleService;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.UpscaleClusterManagerRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.UpscaleClusterManagerResult;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class UpscaleClusterManagerHandler implements EventHandler<UpscaleClusterManagerRequest> {

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
            clusterManagerUpscaleService.upscaleClusterManager(request.getResourceId(), request.getHostGroupName(),
                    request.getScalingAdjustment(), request.isPrimaryGatewayChanged());
            result = new UpscaleClusterManagerResult(request);
        } catch (Exception e) {
            result = new UpscaleClusterManagerResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
