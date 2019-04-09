package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.NotificationEventType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.RecipeV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeViewV4Responses;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.view.RecipeView;
import com.sequenceiq.cloudbreak.service.recipe.RecipeService;
import com.sequenceiq.cloudbreak.util.WorkspaceEntityType;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(Recipe.class)
public class RecipesV4Controller extends NotificationController implements RecipeV4Endpoint {

    @Inject
    private RecipeService recipeService;

    @Inject
    private ConverterUtil converterUtil;

    @Override
    public RecipeViewV4Responses list(Long workspaceId) {
        Set<RecipeView> allViewByWorkspaceId = recipeService.findAllViewByWorkspaceId(workspaceId);
        return new RecipeViewV4Responses(converterUtil.convertAllAsSet(allViewByWorkspaceId, RecipeViewV4Response.class));
    }

    @Override
    public RecipeV4Response get(Long workspaceId, String name) {
        Recipe recipe = recipeService.getByNameForWorkspaceId(name, workspaceId);
        return converterUtil.convert(recipe, RecipeV4Response.class);
    }

    @Override
    public RecipeV4Response post(Long workspaceId, RecipeV4Request request) {
        Recipe recipe = recipeService.createForLoggedInUser(converterUtil.convert(request, Recipe.class), workspaceId);
        RecipeV4Response response = converterUtil.convert(recipe, RecipeV4Response.class);
        notify(response, NotificationEventType.CREATE_SUCCESS, WorkspaceResource.RECIPE, workspaceId);
        return response;
    }

    @Override
    public RecipeV4Response delete(Long workspaceId, String name) {
        Recipe deleted = recipeService.deleteByNameFromWorkspace(name, workspaceId);
        RecipeV4Response response = converterUtil.convert(deleted, RecipeV4Response.class);
        notify(response, NotificationEventType.DELETE_SUCCESS, WorkspaceResource.RECIPE, workspaceId);
        return response;
    }

    @Override
    public RecipeV4Responses deleteMultiple(Long workspaceId, Set<String> names) {
        Set<Recipe> deleted = recipeService.deleteMultipleByNameFromWorkspace(names, workspaceId);
        RecipeV4Responses responses = new RecipeV4Responses(converterUtil.convertAllAsSet(deleted, RecipeV4Response.class));
        notify(responses, NotificationEventType.DELETE_SUCCESS, WorkspaceResource.RECIPE, workspaceId);
        return responses;
    }

    @Override
    public RecipeV4Request getRequest(Long workspaceId, String name) {
        Recipe recipe = recipeService.getByNameForWorkspaceId(name, workspaceId);
        return converterUtil.convert(recipe, RecipeV4Request.class);
    }
}
