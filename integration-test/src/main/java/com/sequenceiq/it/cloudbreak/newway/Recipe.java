package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.sequenceiq.it.IntegrationTestContext;

public class Recipe extends RecipeEntity {

    static Function<IntegrationTestContext, Recipe> getTestContext(String key) {
        return (testContext)->testContext.getContextParam(key, Recipe.class);
    }

    static Function<IntegrationTestContext, Recipe> getNew() {
        return (testContext)->new Recipe();
    }

    public static Recipe request() {
        return new Recipe();
    }

    public static Recipe isCreated() {
        Recipe recipe = new Recipe();
        recipe.setCreationStrategy(RecipeAction::createInGiven);
        return recipe;
    }

    public static Recipe isCreatedDeleted() {
        Recipe recipe = new Recipe();
        recipe.setCreationStrategy(RecipeAction::createDeleteInGiven);
        return recipe;
    }

    public static Action<Recipe> post(String key) {
        return new Action<Recipe>(getTestContext(key), RecipeAction::post);
    }

    public static Action<Recipe> post() {
        return post(RECIPE);
    }

    public static Action<Recipe> get(String key) {
        return new Action<>(getTestContext(key), RecipeAction::get);
    }

    public static Action<Recipe> get() {
        return get(RECIPE);
    }

    public static Action<Recipe> getAll() {
        return new Action<>(getNew(), RecipeAction::getAll);
    }

    public static Action<Recipe> delete(String key) {
        return new Action<>(getTestContext(key), RecipeAction::delete);
    }

    public static Action<Recipe> delete() {
        return delete(RECIPE);
    }

    public static Assertion<Recipe> assertThis(BiConsumer<Recipe, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }
}
