package com.sequenceiq.cloudbreak.reactor.handler.recipe;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.reactor.ClusterEventHandler;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UpscalePostRecipesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UpscalePostRecipesResult;
import com.sequenceiq.cloudbreak.core.cluster.AmbariClusterUpscaleService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class UpscalePostRecipesHandler implements ClusterEventHandler<UpscalePostRecipesRequest> {

    @Inject
    private EventBus eventBus;

    @Inject
    private AmbariClusterUpscaleService clusterUpscaleService;

    @Override
    public Class<UpscalePostRecipesRequest> type() {
        return UpscalePostRecipesRequest.class;
    }

    @Override
    public void accept(Event<UpscalePostRecipesRequest> event) {
        UpscalePostRecipesRequest request = event.getData();
        UpscalePostRecipesResult result;
        try {
            clusterUpscaleService.executePostRecipesOnNewHosts(request.getStackId(), request.getHostGroupName());
            result = new UpscalePostRecipesResult(request);
        } catch (Exception e) {
            result = new UpscalePostRecipesResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
