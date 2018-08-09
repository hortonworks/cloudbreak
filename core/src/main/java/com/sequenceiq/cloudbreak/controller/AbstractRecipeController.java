package com.sequenceiq.cloudbreak.controller;

import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.api.model.RecipeRequest;
import com.sequenceiq.cloudbreak.api.model.RecipeResponse;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.security.Organization;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.recipe.RecipeService;

public abstract class AbstractRecipeController extends NotificationController {

    protected RecipeResponse getRecipeByName(String name) {
        Organization organization = organizationService().getDefaultOrganizationForCurrentUser();
        Recipe recipe = recipeService().getByNameForOrganization(name, organization.getId());
        return conversionService().convert(recipe, RecipeResponse.class);
    }

    protected Set<RecipeResponse> listRecipesByOrganizationId(Long organizationId) {
        return recipeService().listByOrganizationId(organizationId).stream()
                .map(recipe -> conversionService().convert(recipe, RecipeResponse.class))
                .collect(Collectors.toSet());
    }

    protected RecipeResponse createRecipe(@Valid RecipeRequest recipeRequest, Long organizationId) {
        Recipe recipe = conversionService().convert(recipeRequest, Recipe.class);
        IdentityUser identityUser = authenticatedUserService().getCbUser();
        recipe = recipeService().create(identityUser, recipe, organizationId);
        notify(identityUser, ResourceEvent.RECIPE_CREATED);
        return conversionService().convert(recipe, RecipeResponse.class);
    }

    protected RecipeResponse deleteRecipeByNameFromOrg(String name, Long organizationId) {
        Recipe deleted = recipeService().deleteByNameFromOrganization(name, organizationId);
        IdentityUser identityUser = authenticatedUserService().getCbUser();
        notify(identityUser, ResourceEvent.RECIPE_DELETED);
        return conversionService().convert(deleted, RecipeResponse.class);
    }

    // using constructor injection would be better

    protected abstract ConversionService conversionService();

    protected abstract AuthenticatedUserService authenticatedUserService();

    protected abstract OrganizationService organizationService();

    protected abstract RecipeService recipeService();
}
