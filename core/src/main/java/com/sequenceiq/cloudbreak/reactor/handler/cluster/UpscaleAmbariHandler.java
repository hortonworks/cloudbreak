package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.cluster.AmbariClusterUpscaleService;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.UpscaleAmbariRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.UpscaleAmbariResult;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class UpscaleAmbariHandler implements ReactorEventHandler<UpscaleAmbariRequest> {

    @Inject
    private EventBus eventBus;

    @Inject
    private AmbariClusterUpscaleService clusterUpscaleService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpscaleAmbariRequest.class);
    }

    @Override
    public void accept(Event<UpscaleAmbariRequest> event) {
        UpscaleAmbariRequest request = event.getData();
        UpscaleAmbariResult result;
        try {
            clusterUpscaleService.upscaleAmbari(request.getStackId(), request.getHostGroupName(),
                    request.getScalingAdjustment());
            result = new UpscaleAmbariResult(request);
        } catch (Exception e) {
            result = new UpscaleAmbariResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
