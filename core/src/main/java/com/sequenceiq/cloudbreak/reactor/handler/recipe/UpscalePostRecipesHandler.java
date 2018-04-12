package com.sequenceiq.cloudbreak.reactor.handler.recipe;

import com.sequenceiq.cloudbreak.core.cluster.AmbariClusterUpscaleService;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UpscalePostRecipesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UpscalePostRecipesResult;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;
import org.springframework.stereotype.Component;
import reactor.bus.Event;
import reactor.bus.EventBus;

import javax.inject.Inject;

@Component
public class UpscalePostRecipesHandler implements ReactorEventHandler<UpscalePostRecipesRequest> {

    @Inject
    private EventBus eventBus;

    @Inject
    private AmbariClusterUpscaleService clusterUpscaleService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpscalePostRecipesRequest.class);
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
