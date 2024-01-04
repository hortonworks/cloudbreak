package com.sequenceiq.cloudbreak.reactor.handler.recipe;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.cluster.ClusterUpscaleService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UpscalePostRecipesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UpscalePostRecipesResult;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

@Component
public class UpscalePostRecipesHandler implements EventHandler<UpscalePostRecipesRequest> {

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterUpscaleService clusterUpscaleService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpscalePostRecipesRequest.class);
    }

    @Override
    public void accept(Event<UpscalePostRecipesRequest> event) {
        UpscalePostRecipesRequest request = event.getData();
        UpscalePostRecipesResult result;
        try {
            clusterUpscaleService.executePostRecipesOnNewHosts(request.getResourceId(), request.getHostGroupWithAdjustment());
            result = new UpscalePostRecipesResult(request);
        } catch (Exception e) {
            result = new UpscalePostRecipesResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
