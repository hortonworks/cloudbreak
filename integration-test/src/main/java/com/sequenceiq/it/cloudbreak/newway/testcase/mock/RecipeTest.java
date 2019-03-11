package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.client.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class RecipeTest extends AbstractIntegrationTest {

    @Inject
    private RecipeTestClient recipeTestClient;

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        createDefaultUser((TestContext) data[0]);
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
        String spacialName = getNameGenerator().getRandomNameForResource();
        testContext
                .given(RecipeTestDto.class)
                .withName(getNameGenerator().getInvalidRandomNameForResource())
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
        String againName = getNameGenerator().getRandomNameForResource();
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
        String shortName = getNameGenerator().getRandomNameForResource();
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
        String longName = getNameGenerator().getRandomNameForResource();
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
        String longDesc = getNameGenerator().getRandomNameForResource();
        testContext
                .given(RecipeTestDto.class)
                .withDescription(getLongNameGenerator().stringGenerator(1001))
                .when(recipeTestClient.createV4(), key(longDesc))
                .expect(BadRequestException.class, key(longDesc)
                        .withExpectedMessage("size must be between 0 and 1000"))
                .validate();
    }
}
