package com.sequenceiq.cloudbreak.reactor.handler.recipe;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UploadRecipesFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UploadRecipesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UploadRecipesSuccess;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeEngine;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class UploadRecipesHandler implements EventHandler<UploadRecipesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UploadRecipesHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private RecipeEngine recipeEngine;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UploadRecipesRequest.class);
    }

    @Override
    public void accept(Event<UploadRecipesRequest> event) {
        UploadRecipesRequest request = event.getData();
        Selectable result;
        Long stackId = request.getResourceId();
        try {
            recipeEngine.uploadRecipes(stackId, getClass().getSimpleName());
            result = new UploadRecipesSuccess(stackId);
        } catch (Exception e) {
            LOGGER.info("Failed to upload recipes", e);
            result = new UploadRecipesFailed(stackId, e);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
