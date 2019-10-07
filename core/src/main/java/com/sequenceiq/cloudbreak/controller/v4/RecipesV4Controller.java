package com.sequenceiq.cloudbreak.controller.v4;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.dto.RecipeAccessDto.RecipeAccessDtoBuilder.aRecipeAccessDtoBuilder;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Controller;

import com.cloudera.cdp.datahub.model.CreateRecipeRequest;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.workspace.controller.WorkspaceEntityType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.RecipeV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeViewV4Responses;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.view.RecipeView;
import com.sequenceiq.cloudbreak.service.recipe.RecipeService;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(Recipe.class)
public class RecipesV4Controller extends NotificationController implements RecipeV4Endpoint {

    @Inject
    private RecipeService recipeService;

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    @Override
    public RecipeViewV4Responses list(Long workspaceId) {
        Set<RecipeView> allViewByWorkspaceId = recipeService.findAllViewByWorkspaceId(workspaceId);
        return new RecipeViewV4Responses(converterUtil.convertAllAsSet(allViewByWorkspaceId, RecipeViewV4Response.class));
    }

    @Override
    public RecipeV4Response getByName(Long workspaceId, String name) {
        Recipe recipe = recipeService.get(aRecipeAccessDtoBuilder().withName(name).build(), workspaceId);
        return converterUtil.convert(recipe, RecipeV4Response.class);
    }

    @Override
    public RecipeV4Response getByCrn(Long workspaceId, @NotNull String crn) {
        Recipe recipe = recipeService.get(aRecipeAccessDtoBuilder().withCrn(crn).build(), workspaceId);
        return converterUtil.convert(recipe, RecipeV4Response.class);
    }

    @Override
    public RecipeV4Response post(Long workspaceId, RecipeV4Request request) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        String creator = threadBasedUserCrnProvider.getUserCrn();
        Recipe recipeToSave = converterUtil.convert(request, Recipe.class);
        Recipe recipe = recipeService.createForLoggedInUser(recipeToSave, workspaceId, accountId, creator);
        notify(ResourceEvent.RECIPE_CREATED);
        return converterUtil.convert(recipe, RecipeV4Response.class);
    }

    @Override
    public RecipeV4Response deleteByName(Long workspaceId, String name) {
        Recipe deleted = recipeService.delete(aRecipeAccessDtoBuilder().withName(name).build(), workspaceId);
        notify(ResourceEvent.RECIPE_DELETED);
        return converterUtil.convert(deleted, RecipeV4Response.class);
    }

    @Override
    public RecipeV4Response deleteByCrn(Long workspaceId, @NotNull String crn) {
        Recipe deleted = recipeService.delete(aRecipeAccessDtoBuilder().withCrn(crn).build(), workspaceId);
        notify(ResourceEvent.RECIPE_DELETED);
        return converterUtil.convert(deleted, RecipeV4Response.class);
    }

    @Override
    public RecipeV4Responses deleteMultiple(Long workspaceId, Set<String> names) {
        Set<Recipe> deleted = recipeService.deleteMultipleByNameFromWorkspace(names, workspaceId);
        notify(ResourceEvent.RECIPE_DELETED);
        return new RecipeV4Responses(converterUtil.convertAllAsSet(deleted, RecipeV4Response.class));
    }

    @Override
    public RecipeV4Request getRequest(Long workspaceId, String name) {
        Recipe recipe = recipeService.getByNameForWorkspaceId(name, workspaceId);
        return converterUtil.convert(recipe, RecipeV4Request.class);
    }

    @Override
    public CreateRecipeRequest getCreateRecipeRequestForCli(Long workspaceId, RecipeV4Request recipeV4Request) {
        return converterUtil.convert(recipeV4Request, CreateRecipeRequest.class);
    }
}
