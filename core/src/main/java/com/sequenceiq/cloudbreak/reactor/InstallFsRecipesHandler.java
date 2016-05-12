package com.sequenceiq.cloudbreak.reactor;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.InstallFsRecipesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.InstallFsRecipesResult;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterUpscaleService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class InstallFsRecipesHandler implements ClusterEventHandler<InstallFsRecipesRequest> {

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterUpscaleService clusterUpscaleService;

    @Override
    public Class<InstallFsRecipesRequest> type() {
        return InstallFsRecipesRequest.class;
    }

    @Override
    public void accept(Event<InstallFsRecipesRequest> event) {
        InstallFsRecipesRequest request = event.getData();
        InstallFsRecipesResult result;
        try {
            clusterUpscaleService.installFsRecipes(request.getStackId(), request.getHostGroupName());
            result = new InstallFsRecipesResult(request);
        } catch (Exception e) {
            result = new InstallFsRecipesResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
