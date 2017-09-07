package com.sequenceiq.cloudbreak.controller;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.RecipeEndpoint;
import com.sequenceiq.cloudbreak.api.model.RecipeRequest;
import com.sequenceiq.cloudbreak.api.model.RecipeResponse;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.service.recipe.RecipeService;

@Component
public class RecipeController implements RecipeEndpoint {

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Autowired
    private RecipeService recipeService;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @Override
    public RecipeResponse postPublic(RecipeRequest recipeRequest) {
        IdentityUser user = authenticatedUserService.getCbUser();
        return createRecipe(user, recipeRequest, true);
    }

    @Override
    public RecipeResponse postPrivate(RecipeRequest recipeRequest) {
        IdentityUser user = authenticatedUserService.getCbUser();
        return createRecipe(user, recipeRequest, false);
    }

    @Override
    public Set<RecipeResponse> getPrivates() {
        IdentityUser user = authenticatedUserService.getCbUser();
        Set<Recipe> recipes = recipeService.retrievePrivateRecipes(user);
        return toJsonSet(recipes);
    }

    @Override
    public Set<RecipeResponse> getPublics() {
        IdentityUser user = authenticatedUserService.getCbUser();
        Set<Recipe> recipes = recipeService.retrieveAccountRecipes(user);
        return toJsonSet(recipes);
    }

    @Override
    public RecipeResponse getPrivate(String name) {
        IdentityUser user = authenticatedUserService.getCbUser();
        Recipe recipe = recipeService.getPrivateRecipe(name, user);
        return conversionService.convert(recipe, RecipeResponse.class);
    }

    @Override
    public RecipeResponse getPublic(String name) {
        IdentityUser user = authenticatedUserService.getCbUser();
        Recipe recipe = recipeService.getPublicRecipe(name, user);
        return conversionService.convert(recipe, RecipeResponse.class);
    }

    @Override
    public RecipeResponse get(Long id) {
        Recipe recipe = recipeService.get(id);
        return conversionService.convert(recipe, RecipeResponse.class);
    }

    @Override
    public void delete(Long id) {
        IdentityUser user = authenticatedUserService.getCbUser();
        recipeService.delete(id, user);
    }

    @Override
    public void deletePublic(String name) {
        IdentityUser user = authenticatedUserService.getCbUser();
        recipeService.delete(name, user);
    }

    @Override
    public void deletePrivate(String name) {
        IdentityUser user = authenticatedUserService.getCbUser();
        recipeService.delete(name, user);
    }

    private RecipeResponse createRecipe(IdentityUser user, RecipeRequest recipeRequest, boolean publicInAccount) {
        Recipe recipe = conversionService.convert(recipeRequest, Recipe.class);
        recipe.setPublicInAccount(publicInAccount);
        recipe = recipeService.create(user, recipe);
        return conversionService.convert(recipe, RecipeResponse.class);
    }

    private Set<RecipeResponse> toJsonSet(Set<Recipe> recipes) {
        return (Set<RecipeResponse>) conversionService.convert(recipes, TypeDescriptor.forObject(recipes),
                TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(RecipeResponse.class)));
    }
}
