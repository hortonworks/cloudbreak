package com.sequenceiq.cloudbreak.controller;

import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v1.RecipeEndpoint;
import com.sequenceiq.cloudbreak.api.model.RecipeRequest;
import com.sequenceiq.cloudbreak.api.model.RecipeResponse;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.security.Organization;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.recipe.RecipeService;

@Component
@Transactional(TxType.NEVER)
public class RecipeController extends AbstractRecipeController implements RecipeEndpoint {

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Autowired
    private RecipeService recipeService;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @Autowired
    private OrganizationService organizationService;

    @Override
    public RecipeResponse get(Long id) {
        Recipe recipe = recipeService.get(id);
        return conversionService.convert(recipe, RecipeResponse.class);
    }

    @Override
    public void delete(Long id) {
        IdentityUser identityUser = authenticatedUserService.getCbUser();
        Recipe deleted = recipeService.delete(id);
        notify(identityUser, ResourceEvent.RECIPE_DELETED);
        conversionService.convert(deleted, RecipeResponse.class);
    }

    @Override
    public RecipeRequest getRequestfromName(String name) {
        Organization organization = organizationService.getDefaultOrganizationForCurrentUser();
        Recipe recipe = recipeService.getByNameForOrganization(name, organization);
        return conversionService.convert(recipe, RecipeRequest.class);
    }

    @Override
    public RecipeResponse postPublic(RecipeRequest recipeRequest) {
        return createRecipe(recipeRequest, null);
    }

    @Override
    public RecipeResponse postPrivate(RecipeRequest recipeRequest) {
        return createRecipe(recipeRequest, null);
    }

    @Override
    public Set<RecipeResponse> getPrivates() {
        Organization organization = organizationService.getDefaultOrganizationForCurrentUser();
        return listRecipesByOrganizationId(organization.getId());
    }

    @Override
    public Set<RecipeResponse> getPublics() {
        Organization organization = organizationService.getDefaultOrganizationForCurrentUser();
        return listRecipesByOrganizationId(organization.getId());
    }

    @Override
    public RecipeResponse getPrivate(String name) {
        return getRecipeByName(name);
    }

    @Override
    public RecipeResponse getPublic(String name) {
        return getRecipeByName(name);
    }

    @Override
    public void deletePublic(String name) {
        deleteRecipeByNameFromOrg(name, null);
    }

    @Override
    public void deletePrivate(String name) {
        deleteRecipeByNameFromOrg(name, null);
    }

    @Override
    protected ConversionService conversionService() {
        return conversionService;
    }

    @Override
    protected AuthenticatedUserService authenticatedUserService() {
        return authenticatedUserService;
    }

    @Override
    protected OrganizationService organizationService() {
        return organizationService;
    }

    @Override
    protected RecipeService recipeService() {
        return recipeService;
    }
}
