package com.sequenceiq.cloudbreak.controller;

import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.RecipeV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.RecipeRequest;
import com.sequenceiq.cloudbreak.api.model.RecipeResponse;
import com.sequenceiq.cloudbreak.domain.Recipe;

@Controller
@Transactional(TxType.NEVER)
public class RecipeV3Controller extends AbstractRecipeController implements RecipeV3Endpoint {

    @Override
    public Set<RecipeResponse> listRecipesByOrganization(Long organizationId) {
        return listRecipesByOrganizationId(organizationId);
    }

    @Override
    public RecipeResponse getByNameInOrganization(Long organizationId, String recipeName) {
        Recipe recipe = getRecipeService().getByNameForOrganization(recipeName, organizationId);
        return getConversionService().convert(recipe, RecipeResponse.class);
    }

    @Override
    public RecipeResponse createInOrganization(Long organizationId, @Valid RecipeRequest recipeRequest) {
        return createRecipe(recipeRequest, organizationId);
    }

    @Override
    public RecipeResponse deleteInOrganization(Long organizationId, String recipeName) {
        return deleteRecipeByNameFromOrg(recipeName, organizationId);
    }
}
