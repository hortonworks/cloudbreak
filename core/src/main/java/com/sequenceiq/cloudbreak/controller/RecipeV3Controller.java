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
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.recipe.RecipeService;

@Controller
@Transactional(TxType.NEVER)
public class RecipeV3Controller extends NotificationController implements RecipeV3Endpoint {

    @Inject
    private RecipeService recipeService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Override
    public Set<RecipeResponse> listByOrganization(Long organizationId) {
        return recipeService.listByOrganizationId(organizationId).stream()
                .map(recipe -> conversionService.convert(recipe, RecipeResponse.class))
                .collect(Collectors.toSet());
    }

    @Override
    public RecipeResponse getByNameInOrganization(Long organizationId, String name) {
        Recipe recipe = recipeService.getByNameForOrganizationId(name, organizationId);
        return conversionService.convert(recipe, RecipeResponse.class);
    }

    @Override
    public RecipeResponse createInOrganization(Long organizationId, RecipeRequest request) {
        Recipe recipe = conversionService.convert(request, Recipe.class);
        recipe = recipeService.create(recipe, organizationId);
        notify(authenticatedUserService.getCbUser(), ResourceEvent.RECIPE_CREATED);
        return conversionService.convert(recipe, RecipeResponse.class);
    }

    @Override
    public RecipeResponse deleteInOrganization(Long organizationId, String name) {
        Recipe deleted = recipeService.deleteByNameFromOrganization(name, organizationId);
        IdentityUser identityUser = authenticatedUserService.getCbUser();
        notify(identityUser, ResourceEvent.RECIPE_DELETED);
        return conversionService.convert(deleted, RecipeResponse.class);
    }
}
