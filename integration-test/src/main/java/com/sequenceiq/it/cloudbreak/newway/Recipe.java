package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.action.ActionV2;
import com.sequenceiq.it.cloudbreak.newway.action.RecipePostAction;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.v3.RecipeV3Action;

public class Recipe extends RecipeEntity {

    public Recipe() {
    }

    public Recipe(TestContext testContext) {
        super(testContext);
    }

    static Function<IntegrationTestContext, Recipe> getTestContext(String key) {
        return testContext -> testContext.getContextParam(key, Recipe.class);
    }

    static Function<IntegrationTestContext, Recipe> getNew() {
        return testContext -> new Recipe();
    }

    public static Recipe request() {
        return new Recipe();
    }

    public static Recipe isCreated() {
        Recipe recipe = new Recipe();
        recipe.setCreationStrategy(RecipeV3Action::createInGiven);
        return recipe;
    }

    public static Recipe isCreatedDeleted() {
        Recipe recipe = new Recipe();
        recipe.setCreationStrategy(RecipeV3Action::createDeleteInGiven);
        return recipe;
    }

    public static RecipeEntity getByName(TestContext testContext, RecipeEntity entity, CloudbreakClient cloudbreakClient) {
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().recipeV3Endpoint().getByNameInWorkspace(cloudbreakClient.getWorkspaceId(), entity.getName())
        );
        return entity;
    }

    public static Action<Recipe> post(String key) {
        return new Action<>(getTestContext(key), RecipeV3Action::post);
    }

    public static Action<Recipe> post() {
        return post(RECIPE);
    }

    public static Action<Recipe> get(String key) {
        return new Action<>(getTestContext(key), RecipeV3Action::get);
    }

    public static Action<Recipe> get() {
        return get(RECIPE);
    }

    public static Action<Recipe> getAll() {
        return new Action<>(getNew(), RecipeV3Action::getAll);
    }

    public static Action<Recipe> delete(String key) {
        return new Action<>(getTestContext(key), RecipeV3Action::delete);
    }

    public static Action<Recipe> delete() {
        return delete(RECIPE);
    }

    public static Assertion<Recipe> assertThis(BiConsumer<Recipe, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }

    public static ActionV2<RecipeEntity> postV2() {
        return new RecipePostAction();
    }
}