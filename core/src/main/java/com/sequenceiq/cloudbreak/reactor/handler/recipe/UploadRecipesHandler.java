package com.sequenceiq.cloudbreak.reactor.handler.recipe;

import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UploadRecipesFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UploadRecipesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UploadRecipesSuccess;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeEngine;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class UploadRecipesHandler implements ReactorEventHandler<UploadRecipesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UploadRecipesHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private RecipeEngine recipeEngine;

    @Inject
    private StackService stackService;

    @Inject
    private HostGroupService hostGroupService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UploadRecipesRequest.class);
    }

    @Override
    public void accept(Event<UploadRecipesRequest> event) {
        UploadRecipesRequest request = event.getData();
        Selectable result;
        Long stackId = request.getStackId();
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(stackId);
            Set<HostGroup> hostGroups = hostGroupService.getByCluster(stack.getCluster().getId());
            recipeEngine.uploadRecipes(stack, hostGroups);
            result = new UploadRecipesSuccess(stackId);
        } catch (Exception e) {
            LOGGER.info("Failed to upload recipes", e);
            result = new UploadRecipesFailed(stackId, e);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
