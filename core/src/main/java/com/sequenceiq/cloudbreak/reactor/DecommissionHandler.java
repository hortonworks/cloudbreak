package com.sequenceiq.cloudbreak.reactor;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionResult;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterDownscaleService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class DecommissionHandler implements ClusterEventHandler<DecommissionRequest> {

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterDownscaleService clusterDownscaleService;

    @Override
    public Class<DecommissionRequest> type() {
        return DecommissionRequest.class;
    }

    @Override
    public void accept(Event<DecommissionRequest> event) {
        DecommissionRequest request = event.getData();
        DecommissionResult result;
        try {
            Set<String> hostNames = clusterDownscaleService.decommission(request.getStackId(), request.getHostGroupName(), request.getScalingAdjustment());
            result = new DecommissionResult(request, hostNames);
        } catch (Exception e) {
            result = new DecommissionResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
