package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.reactor.ClusterEventHandler;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.UpscaleClusterRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.UpscaleClusterResult;
import com.sequenceiq.cloudbreak.core.cluster.AmbariClusterUpscaleService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class UpscaleClusterHandler implements ClusterEventHandler<UpscaleClusterRequest> {

    @Inject
    private EventBus eventBus;

    @Inject
    private AmbariClusterUpscaleService clusterUpscaleService;

    @Override
    public Class<UpscaleClusterRequest> type() {
        return UpscaleClusterRequest.class;
    }

    @Override
    public void accept(Event<UpscaleClusterRequest> event) {
        UpscaleClusterRequest request = event.getData();
        UpscaleClusterResult result;
        try {
            clusterUpscaleService.installServicesOnNewHosts(request.getStackId(), request.getHostGroupName());
            result = new UpscaleClusterResult(request);
        } catch (Exception e) {
            result = new UpscaleClusterResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
