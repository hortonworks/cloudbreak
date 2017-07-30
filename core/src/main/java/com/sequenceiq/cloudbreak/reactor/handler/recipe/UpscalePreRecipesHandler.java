package com.sequenceiq.cloudbreak.reactor.handler.recipe;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.cluster.AmbariClusterUpscaleService;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UpscalePreRecipesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UpscalePreRecipesResult;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class UpscalePreRecipesHandler implements ReactorEventHandler<UpscalePreRecipesRequest> {

    @Inject
    private EventBus eventBus;

    @Inject
    private AmbariClusterUpscaleService clusterUpscaleService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpscalePreRecipesRequest.class);
    }

    @Override
    public void accept(Event<UpscalePreRecipesRequest> event) {
        UpscalePreRecipesRequest request = event.getData();
        UpscalePreRecipesResult result;
        try {
            clusterUpscaleService.executePreRecipesOnNewHosts(request.getStackId(), request.getHostGroupName());
            result = new UpscalePreRecipesResult(request);
        } catch (Exception e) {
            result = new UpscalePreRecipesResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
