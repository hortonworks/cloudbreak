package com.sequenceiq.cloudbreak.reactor;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.InstallRecipesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.InstallRecipesResult;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterUpscaleService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class InstallRecipesHandler implements ClusterEventHandler<InstallRecipesRequest> {

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterUpscaleService clusterUpscaleService;

    @Override
    public Class<InstallRecipesRequest> type() {
        return InstallRecipesRequest.class;
    }

    @Override
    public void accept(Event<InstallRecipesRequest> event) {
        InstallRecipesRequest request = event.getData();
        InstallRecipesResult result;
        try {
            clusterUpscaleService.installRecipes(request.getStackId(), request.getHostGroupName());
            result = new InstallRecipesResult(request);
        } catch (Exception e) {
            result = new InstallRecipesResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
