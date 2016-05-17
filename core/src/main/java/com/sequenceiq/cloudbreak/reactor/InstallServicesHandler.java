package com.sequenceiq.cloudbreak.reactor;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.InstallServicesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.InstallServicesResult;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterUpscaleService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class InstallServicesHandler implements ClusterEventHandler<InstallServicesRequest> {

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterUpscaleService clusterUpscaleService;

    @Override
    public Class<InstallServicesRequest> type() {
        return InstallServicesRequest.class;
    }

    @Override
    public void accept(Event<InstallServicesRequest> event) {
        InstallServicesRequest request = event.getData();
        InstallServicesResult result;
        try {
            clusterUpscaleService.installServices(request.getStackId(), request.getHostGroupName());
            result = new InstallServicesResult(request);
        } catch (Exception e) {
            result = new InstallServicesResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
