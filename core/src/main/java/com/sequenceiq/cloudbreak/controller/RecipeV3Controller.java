package com.sequenceiq.cloudbreak.controller;

import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.RecipeV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.RecipeRequest;
import com.sequenceiq.cloudbreak.api.model.RecipeResponse;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.recipe.RecipeService;

@Controller
@Transactional(TxType.NEVER)
public class RecipeV3Controller extends AbstractRecipeController implements RecipeV3Endpoint {

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
    public Set<RecipeResponse> listRecipesByOrganization(Long organizationId) {
        return listRecipesByOrganizationId(organizationId);
    }

    @Override
    public RecipeResponse getByNameInOrganization(Long organizationId, String recipeName) {
        Recipe recipe = recipeService.getByNameForOrganization(recipeName, organizationId);
        return conversionService.convert(recipe, RecipeResponse.class);
    }

    @Override
    public RecipeResponse createInOrganization(Long organizationId, @Valid RecipeRequest recipeRequest) {
        return createRecipe(recipeRequest, organizationId);
    }

    @Override
    public RecipeResponse deleteInOrganization(Long organizationId, String recipeName) {
        return deleteRecipeByNameFromOrg(recipeName, organizationId);
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
