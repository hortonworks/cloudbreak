package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import com.sequenceiq.cloudbreak.core.cluster.AmbariClusterUpscaleService;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.UpscaleClusterRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.UpscaleClusterResult;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;
import org.springframework.stereotype.Component;
import reactor.bus.Event;
import reactor.bus.EventBus;

import javax.inject.Inject;

@Component
public class UpscaleClusterHandler implements ReactorEventHandler<UpscaleClusterRequest> {
    @Inject
    private EventBus eventBus;

    @Inject
    private AmbariClusterUpscaleService clusterUpscaleService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpscaleClusterRequest.class);
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
