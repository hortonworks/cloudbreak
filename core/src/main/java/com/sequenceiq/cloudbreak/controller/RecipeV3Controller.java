package com.sequenceiq.cloudbreak.controller;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.RecipeV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.RecipeRequest;
import com.sequenceiq.cloudbreak.api.model.RecipeResponse;
import com.sequenceiq.cloudbreak.api.model.RecipeViewResponse;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.recipe.RecipeService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.util.WorkspaceEntityType;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(Recipe.class)
public class RecipeV3Controller extends NotificationController implements RecipeV3Endpoint {

    @Inject
    private RecipeService recipeService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public Set<RecipeViewResponse> listByWorkspace(Long workspaceId) {
        return recipeService.findAllViewByWorkspaceId(workspaceId).stream()
                .map(recipe -> conversionService.convert(recipe, RecipeViewResponse.class))
                .collect(Collectors.toSet());
    }

    @Override
    public RecipeResponse getByNameInWorkspace(Long workspaceId, String name) {
        Recipe recipe = recipeService.getByNameForWorkspaceId(name, workspaceId);
        return conversionService.convert(recipe, RecipeResponse.class);
    }

    @Override
    public RecipeResponse createInWorkspace(Long workspaceId, RecipeRequest request) {
        Recipe recipe = conversionService.convert(request, Recipe.class);
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        recipe = recipeService.create(recipe, workspaceId, user);
        notify(ResourceEvent.RECIPE_CREATED);
        return conversionService.convert(recipe, RecipeResponse.class);
    }

    @Override
    public RecipeResponse deleteInWorkspace(Long workspaceId, String name) {
        Recipe deleted = recipeService.deleteByNameFromWorkspace(name, workspaceId);
        notify(ResourceEvent.RECIPE_DELETED);
        return conversionService.convert(deleted, RecipeResponse.class);
    }

    @Override
    public RecipeRequest getRequestFromName(Long workspaceId, String name) {
        Recipe recipe = recipeService.getByNameForWorkspaceId(name, workspaceId);
        return conversionService.convert(recipe, RecipeRequest.class);
    }
}
