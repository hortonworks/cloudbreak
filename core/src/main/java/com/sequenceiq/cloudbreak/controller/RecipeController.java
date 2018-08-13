package com.sequenceiq.cloudbreak.controller;

import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v1.RecipeEndpoint;
import com.sequenceiq.cloudbreak.api.model.RecipeRequest;
import com.sequenceiq.cloudbreak.api.model.RecipeResponse;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.security.Organization;

@Component
@Transactional(TxType.NEVER)
public class RecipeController extends AbstractRecipeController implements RecipeEndpoint {

    @Override
    public RecipeResponse get(Long id) {
        Recipe recipe = getRecipeService().get(id);
        return getConversionService().convert(recipe, RecipeResponse.class);
    }

    @Override
    public void delete(Long id) {
        IdentityUser identityUser = getAuthenticatedUserService().getCbUser();
        Recipe deleted = getRecipeService().delete(id);
        notify(identityUser, ResourceEvent.RECIPE_DELETED);
        getConversionService().convert(deleted, RecipeResponse.class);
    }

    @Override
    public RecipeRequest getRequestfromName(String name) {
        Organization organization = getOrganizationService().getDefaultOrganizationForCurrentUser();
        Recipe recipe = getRecipeService().getByNameForOrganization(name, organization);
        return getConversionService().convert(recipe, RecipeRequest.class);
    }

    @Override
    public RecipeResponse postPublic(RecipeRequest recipeRequest) {
        return create(recipeRequest, null);
    }

    @Override
    public RecipeResponse postPrivate(RecipeRequest recipeRequest) {
        return create(recipeRequest, null);
    }

    @Override
    public Set<RecipeResponse> getPrivates() {
        Organization organization = getOrganizationService().getDefaultOrganizationForCurrentUser();
        return listByOrganizationId(organization.getId());
    }

    @Override
    public Set<RecipeResponse> getPublics() {
        Organization organization = getOrganizationService().getDefaultOrganizationForCurrentUser();
        return listByOrganizationId(organization.getId());
    }

    @Override
    public RecipeResponse getPrivate(String name) {
        return getByName(name);
    }

    @Override
    public RecipeResponse getPublic(String name) {
        return getByName(name);
    }

    @Override
    public void deletePublic(String name) {
        deleteByNameFromOrg(name, null);
    }

    @Override
    public void deletePrivate(String name) {
        deleteByNameFromOrg(name, null);
    }
}
