package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.RecipeV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeV4ViewResponse;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.controller.common.NotificationController;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.view.RecipeView;
import com.sequenceiq.cloudbreak.service.recipe.RecipeService;
import com.sequenceiq.cloudbreak.util.ConverterUtil;
import com.sequenceiq.cloudbreak.util.WorkspaceEntityType;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(Recipe.class)
public class RecipesV4Controller extends NotificationController implements RecipeV4Endpoint {

    @Inject
    private RecipeService recipeService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private ConverterUtil converterUtil;

    @Override
    public RecipeV4Responses list(Long workspaceId) {
        Set<RecipeView> allViewByWorkspaceId = recipeService.findAllViewByWorkspaceId(workspaceId);
        return new RecipeV4Responses(converterUtil.convertAllAsSet(allViewByWorkspaceId, RecipeV4ViewResponse.class));
    }

    @Override
    public RecipeV4Response get(Long workspaceId, String name) {
        Recipe recipe = recipeService.getByNameForWorkspaceId(name, workspaceId);
        return conversionService.convert(recipe, RecipeV4Response.class);
    }

    @Override
    public RecipeV4Response post(Long workspaceId, RecipeV4Request request) {
        Recipe recipe = recipeService.createForLoggedInUser(conversionService.convert(request, Recipe.class), workspaceId);
        notify(ResourceEvent.RECIPE_CREATED);
        return conversionService.convert(recipe, RecipeV4Response.class);
    }

    @Override
    public RecipeV4Response delete(Long workspaceId, String name) {
        Recipe deleted = recipeService.deleteByNameFromWorkspace(name, workspaceId);
        notify(ResourceEvent.RECIPE_DELETED);
        return conversionService.convert(deleted, RecipeV4Response.class);
    }

    @Override
    public RecipeV4Request getRequest(Long workspaceId, String name) {
        Recipe recipe = recipeService.getByNameForWorkspaceId(name, workspaceId);
        return conversionService.convert(recipe, RecipeV4Request.class);
    }
}
