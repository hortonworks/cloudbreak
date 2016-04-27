package com.sequenceiq.cloudbreak.reactor;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.WaitForAmbariHostsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.WaitForAmbariHostsResult;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterUpscaleService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class WaitForAmbariHostsHandler implements ClusterEventHandler<WaitForAmbariHostsRequest> {

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterUpscaleService clusterUpscaleService;

    @Override
    public Class<WaitForAmbariHostsRequest> type() {
        return WaitForAmbariHostsRequest.class;
    }

    @Override
    public void accept(Event<WaitForAmbariHostsRequest> event) {
        WaitForAmbariHostsRequest request = event.getData();
        WaitForAmbariHostsResult result;
        try {
            clusterUpscaleService.waitForAmbariHosts(request.getStackId());
            result = new WaitForAmbariHostsResult(request);
        } catch (Exception e) {
            result = new WaitForAmbariHostsResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event(event.getHeaders(), result));
    }
}
