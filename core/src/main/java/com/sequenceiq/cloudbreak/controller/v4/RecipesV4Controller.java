package com.sequenceiq.cloudbreak.controller.v4;

import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.RecipeV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeV4ViewResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeV4ViewResponses;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.controller.common.NotificationController;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.recipe.RecipeService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.util.WorkspaceEntityType;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import java.util.Set;
import java.util.stream.Collectors;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeV4ViewResponses.recipeV4ViewResponses;

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
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public RecipeV4ViewResponses list(Long workspaceId) {
        Set<RecipeV4ViewResponse> recipes = recipeService.findAllViewByWorkspaceId(workspaceId).stream()
                .map(recipe -> conversionService.convert(recipe, RecipeV4ViewResponse.class))
                .collect(Collectors.toSet());
        return recipeV4ViewResponses(recipes);
    }

    @Override
    public RecipeV4Response get(Long workspaceId, String name) {
        Recipe recipe = recipeService.getByNameForWorkspaceId(name, workspaceId);
        return conversionService.convert(recipe, RecipeV4Response.class);
    }

    @Override
    public RecipeV4Response post(Long workspaceId, RecipeV4Request request) {
        Recipe recipe = conversionService.convert(request, Recipe.class);
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        recipe = recipeService.create(recipe, workspaceId, user);
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
