package com.sequenceiq.cloudbreak.controller;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v1.RecipeEndpoint;
import com.sequenceiq.cloudbreak.api.model.RecipeRequest;
import com.sequenceiq.cloudbreak.api.model.RecipeResponse;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.service.recipe.LegacyRecipeService;

@Component
@Transactional(TxType.NEVER)
public class RecipeController extends NotificationController implements RecipeEndpoint {

    @Inject
    private LegacyRecipeService recipeService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Override
    public RecipeResponse get(Long id) {
        return conversionService.convert(recipeService.getByIdFromAnyAvailableOrganization(id), RecipeResponse.class);
    }

    @Override
    public void delete(Long id) {
        Recipe deleted = recipeService.deleteByIdFromAnyAvailableOrganization(id);
        notify(ResourceEvent.RECIPE_DELETED);
        conversionService.convert(deleted, RecipeResponse.class);
    }

    @Override
    public RecipeRequest getRequestfromName(String name) {
        Recipe recipe = recipeService.getByNameFromUsersDefaultOrganization(name);
        return conversionService.convert(recipe, RecipeRequest.class);
    }

    @Override
    public RecipeResponse postPublic(RecipeRequest recipeRequest) {
        return createInDefaultOrganization(recipeRequest);
    }

    @Override
    public RecipeResponse postPrivate(RecipeRequest recipeRequest) {
        return createInDefaultOrganization(recipeRequest);
    }

    @Override
    public Set<RecipeResponse> getPrivates() {
        return listForUsersDefaultOrganization();
    }

    @Override
    public Set<RecipeResponse> getPublics() {
        return listForUsersDefaultOrganization();
    }

    @Override
    public RecipeResponse getPrivate(String name) {
        return getRecipeResponse(name);
    }

    @Override
    public RecipeResponse getPublic(String name) {
        return getRecipeResponse(name);
    }

    @Override
    public void deletePublic(String name) {
        deleteInDefaultOrganization(name);
    }

    @Override
    public void deletePrivate(String name) {
        deleteInDefaultOrganization(name);
    }

    private RecipeResponse getRecipeResponse(String name) {
        return conversionService.convert(recipeService.getByNameFromUsersDefaultOrganization(name), RecipeResponse.class);
    }

    private Set<RecipeResponse> listForUsersDefaultOrganization() {
        return recipeService.findAllForUsersDefaultOrganization().stream()
                .map(recipe -> conversionService.convert(recipe, RecipeResponse.class))
                .collect(Collectors.toSet());
    }

    private void deleteInDefaultOrganization(String name) {
        executeAndNotify(user -> recipeService.deleteByNameFromDefaultOrganization(name), ResourceEvent.RECIPE_DELETED);
    }

    private RecipeResponse createInDefaultOrganization(RecipeRequest request) {
        Recipe recipe = conversionService.convert(request, Recipe.class);
        recipe = recipeService.createInDefaultOrganization(recipe);
        return notifyAndReturn(recipe, ResourceEvent.RECIPE_CREATED);
    }

    private RecipeResponse notifyAndReturn(Recipe recipe, ResourceEvent resourceEvent) {
        notify(resourceEvent);
        return conversionService.convert(recipe, RecipeResponse.class);
    }
}
