package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.cluster.ClusterUpscaleService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.UpscaleClusterRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.UpscaleClusterResult;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class UpscaleClusterHandler implements EventHandler<UpscaleClusterRequest> {
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
        UpscaleClusterRequest request = event.getData();
        UpscaleClusterResult result;
        try {
            clusterUpscaleService.installServicesOnNewHosts(request.getResourceId(), request.getHostGroupNames(),
                    request.isRepair(), request.isRestartServices(), request.getHostGroupsWithHostNames());
            result = new UpscaleClusterResult(request);
        } catch (Exception e) {
            result = new UpscaleClusterResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
