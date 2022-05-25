package com.sequenceiq.cloudbreak.reactor.handler.recipe;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UploadUpscaleRecipesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UploadUpscaleRecipesResult;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class UploadUpscaleRecipesHandler implements EventHandler<UploadUpscaleRecipesRequest> {

    @Inject
    private EventBus eventBus;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UploadUpscaleRecipesRequest.class);
    }

    @Override
    public void accept(Event<UploadUpscaleRecipesRequest> event) {
        UploadUpscaleRecipesRequest request = event.getData();
        UploadUpscaleRecipesResult result;
        try {
            // TODO: because of CB-17116 - step removed - cleanup the code
            result = new UploadUpscaleRecipesResult(request);
        } catch (Exception e) {
            result = new UploadUpscaleRecipesResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
