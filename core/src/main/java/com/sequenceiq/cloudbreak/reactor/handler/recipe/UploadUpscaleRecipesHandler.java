package com.sequenceiq.cloudbreak.reactor.handler.recipe;

import com.sequenceiq.cloudbreak.core.cluster.AmbariClusterUpscaleService;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UploadUpscaleRecipesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UploadUpscaleRecipesResult;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;
import org.springframework.stereotype.Component;
import reactor.bus.Event;
import reactor.bus.EventBus;

import javax.inject.Inject;

@Component
public class UploadUpscaleRecipesHandler implements ReactorEventHandler<UploadUpscaleRecipesRequest> {

    @Inject
    private EventBus eventBus;

    @Inject
    private AmbariClusterUpscaleService clusterUpscaleService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UploadUpscaleRecipesRequest.class);
    }

    @Override
    public void accept(Event<UploadUpscaleRecipesRequest> event) {
        UploadUpscaleRecipesRequest request = event.getData();
        UploadUpscaleRecipesResult result;
        try {
            clusterUpscaleService.uploadRecipesOnNewHosts(request.getStackId(), request.getHostGroupName());
            result = new UploadUpscaleRecipesResult(request);
        } catch (Exception e) {
            result = new UploadUpscaleRecipesResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
