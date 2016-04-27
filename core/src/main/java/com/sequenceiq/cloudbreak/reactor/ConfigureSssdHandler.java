package com.sequenceiq.cloudbreak.reactor;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.ConfigureSssdRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ConfigureSssdResult;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterUpscaleService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ConfigureSssdHandler implements ClusterEventHandler<ConfigureSssdRequest> {

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterUpscaleService clusterUpscaleService;

    @Override
    public Class<ConfigureSssdRequest> type() {
        return ConfigureSssdRequest.class;
    }

    @Override
    public void accept(Event<ConfigureSssdRequest> event) {
        ConfigureSssdRequest request = event.getData();
        ConfigureSssdResult result;
        try {
            clusterUpscaleService.configureSssd(request.getStackId(), request.getHostGroupName());
            result = new ConfigureSssdResult(request);
        } catch (Exception e) {
            result = new ConfigureSssdResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event(event.getHeaders(), result));
    }
}
