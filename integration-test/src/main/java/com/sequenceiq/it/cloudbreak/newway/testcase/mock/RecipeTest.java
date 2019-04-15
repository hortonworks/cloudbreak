package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertThat;

import java.util.Collection;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.client.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class RecipeTest extends AbstractIntegrationTest {

    @Inject
    private RecipeTestClient recipeTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "a valid recipe request",
            when = "create recipe",
            then = "recipe created")
    public void testCreateValidRecipe(TestContext testContext) {
        testContext
                .given(RecipeTestDto.class)
                .when(recipeTestClient.createV4())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "a valid recipe request",
            when = "create recipe and then delete and create again",
            then = "recipe created")
    public void testCreateDeleteValidCreateAgain(TestContext testContext) {
        testContext
                .given(RecipeTestDto.class)
                .when(recipeTestClient.createV4())
                .when(recipeTestClient.deleteV4())
                .when(recipeTestClient.createV4())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "an invalid recipe request with specific characters in name",
            when = "create recipe",
            then = "getting a BadRequestException")
    public void testCreateSpecialNameRecipe(TestContext testContext) {
        String spacialName = resourcePropertyProvider().getName();
        testContext
                .given(RecipeTestDto.class)
                .withName(resourcePropertyProvider().getInvalidName())
                .when(recipeTestClient.createV4(), key(spacialName))
                .expect(BadRequestException.class, key(spacialName)
                        .withExpectedMessage("The recipe's name can only contain lowercase alphanumeric characters and hyphens and has start "
                                + "with an alphanumeric character"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "a valid recipe request",
            when = "create recipe twice",
            then = "getting a BadRequestException")
    public void testCreateAgainRecipe(TestContext testContext) {
        String againName = resourcePropertyProvider().getName();
        testContext.given(RecipeTestDto.class)
                .when(recipeTestClient.createV4())
                .when(recipeTestClient.createV4(), key(againName))
                .expect(BadRequestException.class, key(againName)
                        .withExpectedMessage("recipe already exists with name '"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "an invalid recipe request with too short name",
            when = "create recipe",
            then = "getting a BadRequestException")
    public void testCreateInvalidRecipeShortName(TestContext testContext) {
        String shortName = resourcePropertyProvider().getName();
        testContext
                .given(RecipeTestDto.class)
                .withName(getLongNameGenerator().stringGenerator(3))
                .when(recipeTestClient.createV4(), key(shortName))
                .expect(BadRequestException.class, key(shortName)
                        .withExpectedMessage("The length of the recipe's name has to be in range of 5 to 100"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "an invalid recipe request with too long name",
            when = "create recipe",
            then = "getting a BadRequestException")
    public void testCreateInvalidRecipeLongName(TestContext testContext) {
        String longName = resourcePropertyProvider().getName();
        testContext
                .given(RecipeTestDto.class)
                .withName(getLongNameGenerator().stringGenerator(101))
                .when(recipeTestClient.createV4(), key(longName))
                .expect(BadRequestException.class, key(longName)
                        .withExpectedMessage("The length of the recipe's name has to be in range of 5 to 100"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "an invalid recipe request with too long description",
            when = "create recipe",
            then = "getting a BadRequestException")
    public void testCreateInvalidRecipeLongDescription(TestContext testContext) {
        String longDesc = resourcePropertyProvider().getName();
        testContext
                .given(RecipeTestDto.class)
                .withDescription(getLongNameGenerator().stringGenerator(1001))
                .when(recipeTestClient.createV4(), key(longDesc))
                .expect(BadRequestException.class, key(longDesc)
                        .withExpectedMessage("size must be between 0 and 1000"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "a deleted valid recipe",
            when = "list recipes",
            then = "recipe is not present")
    public void testCreateDeleteValidAndList(TestContext testContext) {
        String recipeName = resourcePropertyProvider().getName();
        testContext
                .given(RecipeTestDto.class)
                .withName(recipeName)
                .when(recipeTestClient.createV4())
                .when(recipeTestClient.deleteV4())

                .when(recipeTestClient.listV4());

        testContext
                .given(RecipeTestDto.class)
                .when((tc, recipeTestDto, cc) -> assertRecipesEmpty(tc, recipeTestDto, cc, recipeName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "a valid recipe",
            when = "list recipes",
            then = "recipe is present")
    public void testCreateValidAndList(TestContext testContext) {
        String recipeName = resourcePropertyProvider().getName();
        testContext
                .given(RecipeTestDto.class)
                .withName(recipeName)
                .when(recipeTestClient.createV4())

                .when(recipeTestClient.listV4());

        testContext
                .given(RecipeTestDto.class)
                .when((tc, recipeTestDto, cc) -> assertRecipesContainsElement(tc, recipeTestDto, cc, recipeName))
                .validate();
    }

    private RecipeTestDto assertRecipesEmpty(TestContext tc, RecipeTestDto recipeTestDto, CloudbreakClient cc, String recipeName) {
        Collection<?> foundRecipes = recipeTestDto.getSimpleResponses().getResponses();
        assertThat(foundRecipes, not(hasItem(hasProperty("name", is(recipeName)))));
        return recipeTestDto;
    }

    private RecipeTestDto assertRecipesContainsElement(TestContext tc, RecipeTestDto recipeTestDto, CloudbreakClient cc, String recipeName) {
        Collection<?> foundRecipes = recipeTestDto.getSimpleResponses().getResponses();
        assertThat(foundRecipes, hasItem(hasProperty("name", is(recipeName))));
        return recipeTestDto;
    }

}
