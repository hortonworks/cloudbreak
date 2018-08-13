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
    public Set<RecipeResponse> listByOrganization(Long organizationId) {
        return listByOrganizationId(organizationId);
    }

    @Override
    public RecipeResponse getByNameInOrganization(Long organizationId, String name) {
        Recipe recipe = getRecipeService().getByNameForOrganization(name, organizationId);
        return getConversionService().convert(recipe, RecipeResponse.class);
    }

    @Override
    public RecipeResponse createInOrganization(Long organizationId, @Valid RecipeRequest request) {
        return create(request, organizationId);
    }

    @Override
    public RecipeResponse deleteInOrganization(Long organizationId, String name) {
        return deleteByNameFromOrg(name, organizationId);
    }
}
