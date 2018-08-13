package com.sequenceiq.cloudbreak.controller;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
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

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    private OrganizationService organizationService;

    @Inject
    private RecipeService recipeService;

    protected RecipeResponse getByName(String name) {
        Organization organization = organizationService.getDefaultOrganizationForCurrentUser();
        Recipe recipe = recipeService.getByNameForOrganization(name, organization.getId());
        return conversionService.convert(recipe, RecipeResponse.class);
    }

    protected Set<RecipeResponse> listByOrganizationId(Long organizationId) {
        return recipeService.listByOrganizationId(organizationId).stream()
                .map(recipe -> conversionService.convert(recipe, RecipeResponse.class))
                .collect(Collectors.toSet());
    }

    protected RecipeResponse create(@Valid RecipeRequest request, Long organizationId) {
        Recipe recipe = conversionService.convert(request, Recipe.class);
        recipe = recipeService.create(recipe, organizationId);
        notify(authenticatedUserService.getCbUser(), ResourceEvent.RECIPE_CREATED);
        return conversionService.convert(recipe, RecipeResponse.class);
    }

    protected RecipeResponse deleteByNameFromOrg(String name, Long organizationId) {
        Recipe deleted = recipeService.deleteByNameFromOrganization(name, organizationId);
        IdentityUser identityUser = authenticatedUserService.getCbUser();
        notify(identityUser, ResourceEvent.RECIPE_DELETED);
        return conversionService.convert(deleted, RecipeResponse.class);
    }

    public ConversionService getConversionService() {
        return conversionService;
    }

    public AuthenticatedUserService getAuthenticatedUserService() {
        return authenticatedUserService;
    }

    public OrganizationService getOrganizationService() {
        return organizationService;
    }

    public RecipeService getRecipeService() {
        return recipeService;
    }
}
