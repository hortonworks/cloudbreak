package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Controller;

import com.cloudera.cdp.datahub.model.CreateRecipeRequest;
import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceNameList;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.FilterListBasedOnPermissions;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.annotation.ResourceNameList;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.RecipeV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeViewV4Responses;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareParam;
import com.sequenceiq.cloudbreak.authorization.RecipeFiltering;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
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
    private ConverterUtil converterUtil;

    @Inject
    private RecipeFiltering recipeFiltering;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    @FilterListBasedOnPermissions(action = AuthorizationResourceAction.DESCRIBE_RECIPE, filter = RecipeFiltering.class)
    public RecipeViewV4Responses list(Long workspaceId) {
        Set<RecipeView> allViewByWorkspaceId = recipeFiltering.filterRecipes(AuthorizationResourceAction.DESCRIBE_RECIPE);
        return new RecipeViewV4Responses(converterUtil.convertAllAsSet(allViewByWorkspaceId, RecipeViewV4Response.class));
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_RECIPE)
    public RecipeV4Response getByName(Long workspaceId, @ResourceName String name) {
        Recipe recipe = recipeService.get(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId());
        return converterUtil.convert(recipe, RecipeV4Response.class);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_RECIPE)
    public RecipeV4Response getByCrn(Long workspaceId, @TenantAwareParam @NotNull @ResourceCrn String crn) {
        Recipe recipe = recipeService.get(NameOrCrn.ofCrn(crn), restRequestThreadLocalService.getRequestedWorkspaceId());
        return converterUtil.convert(recipe, RecipeV4Response.class);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_RECIPE)
    public RecipeV4Response post(Long workspaceId, RecipeV4Request request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String creator = ThreadBasedUserCrnProvider.getUserCrn();
        Recipe recipeToSave = converterUtil.convert(request, Recipe.class);
        Recipe recipe = recipeService.createForLoggedInUser(recipeToSave, restRequestThreadLocalService.getRequestedWorkspaceId(), accountId, creator);
        notify(ResourceEvent.RECIPE_CREATED);
        return converterUtil.convert(recipe, RecipeV4Response.class);
    }

    @Override
    @InternalOnly
    public RecipeV4Response postInternal(@AccountId String accountId, Long workspaceId, @Valid RecipeV4Request request) {
        Recipe recipeToSave = converterUtil.convert(request, Recipe.class);
        Recipe recipe = recipeService.createWithInternalUser(recipeToSave,
                restRequestThreadLocalService.getRequestedWorkspaceId(), accountId);
        notify(ResourceEvent.RECIPE_CREATED);
        return converterUtil.convert(recipe, RecipeV4Response.class);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DELETE_RECIPE)
    public RecipeV4Response deleteByName(Long workspaceId, @ResourceName String name) {
        Recipe deleted = recipeService.delete(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId());
        notify(ResourceEvent.RECIPE_DELETED);
        return converterUtil.convert(deleted, RecipeV4Response.class);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DELETE_RECIPE)
    public RecipeV4Response deleteByCrn(Long workspaceId, @NotNull @ResourceCrn String crn) {
        Recipe deleted = recipeService.delete(NameOrCrn.ofCrn(crn), restRequestThreadLocalService.getRequestedWorkspaceId());
        notify(ResourceEvent.RECIPE_DELETED);
        return converterUtil.convert(deleted, RecipeV4Response.class);
    }

    @Override
    @CheckPermissionByResourceNameList(action = AuthorizationResourceAction.DELETE_RECIPE)
    public RecipeV4Responses deleteMultiple(Long workspaceId, @ResourceNameList Set<String> names) {
        Set<Recipe> deleted = recipeService.deleteMultipleByNameFromWorkspace(names, restRequestThreadLocalService.getRequestedWorkspaceId());
        notify(ResourceEvent.RECIPE_DELETED);
        return new RecipeV4Responses(converterUtil.convertAllAsSet(deleted, RecipeV4Response.class));
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_RECIPE)
    public RecipeV4Request getRequest(Long workspaceId, @ResourceName String name) {
        Recipe recipe = recipeService.getByNameForWorkspaceId(name, restRequestThreadLocalService.getRequestedWorkspaceId());
        return converterUtil.convert(recipe, RecipeV4Request.class);
    }

    @Override
    @DisableCheckPermissions
    public CreateRecipeRequest getCreateRecipeRequestForCli(Long workspaceId, RecipeV4Request recipeV4Request) {
        return converterUtil.convert(recipeV4Request, CreateRecipeRequest.class);
    }
}
