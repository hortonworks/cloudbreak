package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceNameList;
import com.sequenceiq.authorization.annotation.FilterListBasedOnPermissions;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.annotation.ResourceNameList;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.RecipeV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeViewV4Responses;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.auth.security.internal.InitiatorUserCrn;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.authorization.RecipeFiltering;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.converter.v4.recipes.RecipeToRecipeV4RequestConverter;
import com.sequenceiq.cloudbreak.converter.v4.recipes.RecipeToRecipeV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.recipes.RecipeV4RequestToRecipeConverter;
import com.sequenceiq.cloudbreak.converter.v4.recipes.RecipeViewToRecipeV4ViewResponseConverter;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.view.RecipeView;
import com.sequenceiq.cloudbreak.service.recipe.RecipeService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.workspace.controller.WorkspaceEntityType;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(Recipe.class)
public class RecipesV4Controller extends NotificationController implements RecipeV4Endpoint {

    @Inject
    private RecipeService recipeService;

    @Inject
    private RecipeFiltering recipeFiltering;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private RecipeViewToRecipeV4ViewResponseConverter recipeViewToRecipeV4ViewResponseConverter;

    @Inject
    private RecipeToRecipeV4ResponseConverter recipeToRecipeV4ResponseConverter;

    @Inject
    private RecipeToRecipeV4RequestConverter recipeToRecipeV4RequestConverter;

    @Inject
    private RecipeV4RequestToRecipeConverter recipeV4RequestToRecipeConverter;

    @Override
    @FilterListBasedOnPermissions
    public RecipeViewV4Responses list(Long workspaceId) {
        Set<RecipeView> allViewByWorkspaceId = recipeFiltering.filterRecipes(AuthorizationResourceAction.DESCRIBE_RECIPE);
        return new RecipeViewV4Responses(
                allViewByWorkspaceId.stream()
                .map(r -> recipeViewToRecipeV4ViewResponseConverter.convert(r))
                .collect(Collectors.toSet())
        );
    }

    @Override
    @InternalOnly
    public RecipeViewV4Responses listInternal(Long workspaceId, @InitiatorUserCrn String initiatorUserCrn) {
        Set<RecipeView> allViewByWorkspaceId = recipeService.findAllViewByWorkspaceId(restRequestThreadLocalService.getRequestedWorkspaceId());
        return new RecipeViewV4Responses(
                allViewByWorkspaceId.stream()
                        .map(r -> recipeViewToRecipeV4ViewResponseConverter.convert(r))
                        .collect(Collectors.toSet())
        );
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_RECIPE)
    public RecipeV4Response getByName(Long workspaceId, @ResourceName String name) {
        Recipe recipe = recipeService.get(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId());
        return recipeToRecipeV4ResponseConverter.convert(recipe);
    }

    @Override
    @InternalOnly
    public RecipeV4Response getByNameInternal(Long workspaceId, @AccountId String accountId, String name) {
        return getByName(restRequestThreadLocalService.getRequestedWorkspaceId(), name);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_RECIPE)
    public RecipeV4Response getByCrn(Long workspaceId, @ResourceCrn String crn) {
        Recipe recipe = recipeService.get(NameOrCrn.ofCrn(crn), restRequestThreadLocalService.getRequestedWorkspaceId());
        return recipeToRecipeV4ResponseConverter.convert(recipe);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_RECIPE)
    public RecipeV4Response post(Long workspaceId, RecipeV4Request request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String creator = ThreadBasedUserCrnProvider.getUserCrn();
        Recipe recipeToSave = recipeV4RequestToRecipeConverter.convert(request, accountId);
        Recipe recipe = recipeService.createForLoggedInUser(recipeToSave, restRequestThreadLocalService.getRequestedWorkspaceId(), accountId, creator);
        notify(ResourceEvent.RECIPE_CREATED);
        return recipeToRecipeV4ResponseConverter.convert(recipe);
    }

    @Override
    @InternalOnly
    public RecipeV4Response postInternal(@AccountId String accountId, Long workspaceId, RecipeV4Request request) {
        Recipe recipeToSave = recipeV4RequestToRecipeConverter.convert(request, accountId);
        Recipe recipe = recipeService.createWithInternalUser(recipeToSave,
                restRequestThreadLocalService.getRequestedWorkspaceId(), accountId);
        notify(ResourceEvent.RECIPE_CREATED);
        return recipeToRecipeV4ResponseConverter.convert(recipe);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DELETE_RECIPE)
    public RecipeV4Response deleteByName(Long workspaceId, @ResourceName String name) {
        Recipe deleted = recipeService.delete(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId());
        notify(ResourceEvent.RECIPE_DELETED);
        return recipeToRecipeV4ResponseConverter.convert(deleted);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DELETE_RECIPE)
    public RecipeV4Response deleteByCrn(Long workspaceId, @ResourceCrn String crn) {
        Recipe deleted = recipeService.delete(NameOrCrn.ofCrn(crn), restRequestThreadLocalService.getRequestedWorkspaceId());
        notify(ResourceEvent.RECIPE_DELETED);
        return recipeToRecipeV4ResponseConverter.convert(deleted);
    }

    @Override
    @CheckPermissionByResourceNameList(action = AuthorizationResourceAction.DELETE_RECIPE)
    public RecipeV4Responses deleteMultiple(Long workspaceId, @ResourceNameList Set<String> names) {
        Set<Recipe> deleted = recipeService.deleteMultipleByNameFromWorkspace(names, restRequestThreadLocalService.getRequestedWorkspaceId());
        notify(ResourceEvent.RECIPE_DELETED);
        return new RecipeV4Responses(deleted.stream()
                .map(r -> recipeToRecipeV4ResponseConverter.convert(r))
                .collect(Collectors.toSet())
        );
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_RECIPE)
    public RecipeV4Request getRequest(Long workspaceId, @ResourceName String name) {
        Recipe recipe = recipeService.getByNameForWorkspaceId(name, restRequestThreadLocalService.getRequestedWorkspaceId());
        return recipeToRecipeV4RequestConverter.convert(recipe);
    }

    @Override
    @CheckPermissionByResourceNameList(action = AuthorizationResourceAction.DESCRIBE_RECIPE)
    public Set<RecipeV4Request> getRequestsByNames(Long workspaceId, @ResourceNameList Set<String> names, @InitiatorUserCrn String initiatorUserCrn) {
        Set<Recipe> recipes = recipeService.getByNamesForWorkspaceId(names, restRequestThreadLocalService.getRequestedWorkspaceId());
        return recipes.stream().map(recipe -> recipeToRecipeV4RequestConverter.convert(recipe)).collect(Collectors.toSet());
    }

}
