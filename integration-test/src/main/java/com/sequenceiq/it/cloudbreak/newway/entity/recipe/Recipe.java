package com.sequenceiq.it.cloudbreak.newway.entity.recipe;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.Assertion;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.GherkinTest;
import com.sequenceiq.it.cloudbreak.newway.ResourceAction;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.action.recipe.RecipePostAction;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.v4.RecipeV4Action;

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
        recipe.setCreationStrategy(RecipeV4Action::createInGiven);
        return recipe;
    }

    public static Recipe isCreatedDeleted() {
        Recipe recipe = new Recipe();
        recipe.setCreationStrategy(RecipeV4Action::createDeleteInGiven);
        return recipe;
    }

    public static RecipeEntity getByName(TestContext testContext, RecipeEntity entity, CloudbreakClient cloudbreakClient) {
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().recipeV4Endpoint().get(cloudbreakClient.getWorkspaceId(), entity.getName())
        );
        return entity;
    }

    public static ResourceAction<Recipe> post(String key) {
        return new ResourceAction<>(getTestContext(key), RecipeV4Action::post);
    }

    public static ResourceAction<Recipe> post() {
        return post(RECIPE);
    }

    public static ResourceAction<Recipe> get(String key) {
        return new ResourceAction<>(getTestContext(key), RecipeV4Action::get);
    }

    public static ResourceAction<Recipe> get() {
        return get(RECIPE);
    }

    public static ResourceAction<Recipe> getAll() {
        return new ResourceAction<>(getNew(), RecipeV4Action::getAll);
    }

    public static ResourceAction<Recipe> delete(String key) {
        return new ResourceAction<>(getTestContext(key), RecipeV4Action::delete);
    }

    public static ResourceAction<Recipe> delete() {
        return delete(RECIPE);
    }

    public static Assertion<Recipe> assertThis(BiConsumer<Recipe, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }

    public static Action<RecipeEntity> postV4() {
        return new RecipePostAction();
    }
}