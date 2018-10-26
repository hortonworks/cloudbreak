package com.sequenceiq.cloudbreak.controller;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v1.RecipeEndpoint;
import com.sequenceiq.cloudbreak.api.model.RecipeRequest;
import com.sequenceiq.cloudbreak.api.model.RecipeResponse;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.recipe.RecipeService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Controller
@Transactional(TxType.NEVER)
public class RecipeController extends NotificationController implements RecipeEndpoint {

    @Inject
    private RecipeService recipeService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private UserService userService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public RecipeResponse get(Long id) {
        return conversionService.convert(recipeService.get(id), RecipeResponse.class);
    }

    @Override
    public void delete(Long id) {
        Recipe deleted = recipeService.delete(id);
        notify(ResourceEvent.RECIPE_DELETED);
        conversionService.convert(deleted, RecipeResponse.class);
    }

    @Override
    public RecipeRequest getRequestfromName(String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        Recipe recipe = recipeService.getByNameForWorkspace(name, workspace);
        return conversionService.convert(recipe, RecipeRequest.class);
    }

    @Override
    public RecipeResponse postPublic(RecipeRequest recipeRequest) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        return createInDefaultWorkspace(recipeRequest, user);
    }

    @Override
    public RecipeResponse postPrivate(RecipeRequest recipeRequest) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        return createInDefaultWorkspace(recipeRequest, user);
    }

    @Override
    public Set<RecipeResponse> getPrivates() {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        return listForUsersDefaultWorkspace(user);
    }

    @Override
    public Set<RecipeResponse> getPublics() {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        return listForUsersDefaultWorkspace(user);
    }

    @Override
    public RecipeResponse getPrivate(String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        return getRecipeResponse(name, user);
    }

    @Override
    public RecipeResponse getPublic(String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        return getRecipeResponse(name, user);
    }

    @Override
    public void deletePublic(String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        deleteInDefaultWorkspace(name, user);
    }

    @Override
    public void deletePrivate(String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        deleteInDefaultWorkspace(name, user);
    }

    private RecipeResponse getRecipeResponse(String name, User user) {
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        return conversionService.convert(recipeService.getByNameForWorkspace(name, workspace), RecipeResponse.class);
    }

    private Set<RecipeResponse> listForUsersDefaultWorkspace(User user) {
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        return recipeService.findAllByWorkspace(workspace).stream()
                .map(recipe -> conversionService.convert(recipe, RecipeResponse.class))
                .collect(Collectors.toSet());
    }

    private void deleteInDefaultWorkspace(String name, User user) {
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        executeAndNotify(identityUser -> recipeService.deleteByNameFromWorkspace(name, workspace.getId()), ResourceEvent.RECIPE_DELETED);
    }

    private RecipeResponse createInDefaultWorkspace(RecipeRequest request, User user) {
        Recipe recipe = conversionService.convert(request, Recipe.class);
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        recipe = recipeService.create(recipe, workspace, user);
        return notifyAndReturn(recipe, ResourceEvent.RECIPE_CREATED);
    }

    private RecipeResponse notifyAndReturn(Recipe recipe, ResourceEvent resourceEvent) {
        notify(resourceEvent);
        return conversionService.convert(recipe, RecipeResponse.class);
    }
}
