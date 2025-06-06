package com.sequenceiq.freeipa.controller;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_RECIPE;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.EDIT_ENVIRONMENT;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.CRN;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.NAME_LIST;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByRequestProperty;
import com.sequenceiq.cloudbreak.auth.security.internal.RequestObject;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.freeipa.api.v1.recipe.RecipeV1Endpoint;
import com.sequenceiq.freeipa.api.v1.recipe.model.RecipeAttachDetachRequest;
import com.sequenceiq.freeipa.service.recipe.FreeIpaRecipeService;
import com.sequenceiq.freeipa.util.CrnService;

@Controller
public class RecipeV1Controller implements RecipeV1Endpoint {

    @Inject
    private FreeIpaRecipeService freeIpaRecipeService;

    @Inject
    private CrnService crnService;

    @Override
    @CheckPermissionByRequestProperty(path = "environmentCrn", type = CRN, action = EDIT_ENVIRONMENT)
    @CheckPermissionByRequestProperty(path = "recipes", type = NAME_LIST, action = DESCRIBE_RECIPE)
    public void attachRecipes(@RequestObject RecipeAttachDetachRequest recipeAttach) {
        MDCBuilder.addEnvironmentCrn(recipeAttach.getEnvironmentCrn());
        String accountId = crnService.getCurrentAccountId();
        freeIpaRecipeService.attachRecipes(accountId, recipeAttach);
    }

    @Override
    @CheckPermissionByRequestProperty(path = "environmentCrn", type = CRN, action = EDIT_ENVIRONMENT)
    public void detachRecipes(@RequestObject RecipeAttachDetachRequest recipeDetach) {
        MDCBuilder.addEnvironmentCrn(recipeDetach.getEnvironmentCrn());
        String accountId = crnService.getCurrentAccountId();
        freeIpaRecipeService.detachRecipes(accountId, recipeDetach);
    }

}
