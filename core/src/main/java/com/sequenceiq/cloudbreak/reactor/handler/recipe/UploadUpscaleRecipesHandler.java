package com.sequenceiq.cloudbreak.reactor.handler.recipe;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UploadUpscaleRecipesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UploadUpscaleRecipesResult;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeEngine;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

@Component
public class UploadUpscaleRecipesHandler implements EventHandler<UploadUpscaleRecipesRequest> {

    @Inject
    private EventBus eventBus;

    @Inject
    private RecipeEngine recipeEngine;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UploadUpscaleRecipesRequest.class);
    }

    @Override
    public void accept(Event<UploadUpscaleRecipesRequest> event) {
        UploadUpscaleRecipesRequest request = event.getData();
        UploadUpscaleRecipesResult result;
        try {
            recipeEngine.uploadRecipes(request.getResourceId());
            result = new UploadUpscaleRecipesResult(request);
        } catch (Exception e) {
            result = new UploadUpscaleRecipesResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
