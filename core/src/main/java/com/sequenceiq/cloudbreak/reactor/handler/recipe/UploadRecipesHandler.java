package com.sequenceiq.cloudbreak.reactor.handler.recipe;

import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UploadRecipesFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UploadRecipesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UploadRecipesSuccess;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeEngine;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
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

    @Inject
    private StackService stackService;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private ResourceService resourceService;

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
            LOGGER.info("Upload recipes started for {} stack", stackId);
            Stack stack = measure(() -> stackService.getByIdWithListsInTransaction(stackId), LOGGER,
                    "stackService.getByIdWithListsInTransaction() took {} ms in UploadRecipesHandler");
            stack.setResources(measure(() -> resourceService.getNotInstanceRelatedByStackId(stackId), LOGGER,
                    "resourceService.getNotInstanceRelatedByStackId() took {} ms in UploadRecipesHandler"));
            Set<HostGroup> hostGroups = measure(() -> hostGroupService.getByClusterWithRecipes(stack.getCluster().getId()), LOGGER,
                    "hostGroupService.getByClusterWithRecipes() took {} ms in UploadRecipesHandler");
            recipeEngine.uploadRecipes(stack, hostGroups);
            LOGGER.info("Upload recipes finished successfully for {} stack", stackId);
            result = new UploadRecipesSuccess(stackId);
        } catch (Exception e) {
            LOGGER.info("Failed to upload recipes", e);
            result = new UploadRecipesFailed(stackId, e);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
