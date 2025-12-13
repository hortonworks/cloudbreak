package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.expectedMessage;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsCollectionContaining.hasItem;

import java.util.Collection;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.assertion.audit.RecipeAuditGrpcServiceAssertion;
import com.sequenceiq.it.cloudbreak.client.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class RecipeTest extends AbstractMockTest {

    @Inject
    private RecipeTestClient recipeTestClient;

    @Inject
    private RecipeAuditGrpcServiceAssertion recipeAuditGrpcServiceAssertion;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a valid recipe request",
            when = "create recipe",
            then = "recipe created")
    public void testCreateValidRecipe(MockedTestContext testContext) {
        testContext
                .given(RecipeTestDto.class)
                .when(recipeTestClient.createV4())
                .when(recipeTestClient.deleteV4())
                .then(recipeAuditGrpcServiceAssertion::create)
                .then(recipeAuditGrpcServiceAssertion::delete)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a valid recipe request",
            when = "create recipe and then delete and create again",
            then = "recipe created")
    public void testCreateDeleteValidCreateAgain(MockedTestContext testContext) {
        testContext
                .given(RecipeTestDto.class)
                .when(recipeTestClient.createV4())
                .when(recipeTestClient.deleteV4())
                .when(recipeTestClient.createV4())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "an invalid recipe request with specific characters in name",
            when = "create recipe",
            then = "getting a BadRequestException")
    public void testCreateSpecialNameRecipe(MockedTestContext testContext) {
        testContext
                .given(RecipeTestDto.class)
                .withName(resourcePropertyProvider().getInvalidName())
                .whenException(recipeTestClient.createV4(), BadRequestException.class, expectedMessage("The recipe's name can only contain lowercase" +
                        " alphanumeric characters and hyphens and has start with an alphanumeric character"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a valid recipe request",
            when = "create recipe twice",
            then = "getting a BadRequestException")
    public void testCreateAgainRecipe(MockedTestContext testContext) {
        testContext.given(RecipeTestDto.class)
                .when(recipeTestClient.createV4())
                .whenException(recipeTestClient.createV4(), BadRequestException.class, expectedMessage("recipe already exists with name '"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "an invalid recipe request with too short name",
            when = "create recipe",
            then = "getting a BadRequestException")
    public void testCreateInvalidRecipeShortName(MockedTestContext testContext) {
        testContext
                .given(RecipeTestDto.class)
                .withName(getLongNameGenerator().stringGenerator(3))
                .whenException(recipeTestClient.createV4(), BadRequestException.class, expectedMessage("The length of the recipe's name has to be in range" +
                        " of 5 to 100"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "an invalid recipe request with too long name",
            when = "create recipe",
            then = "getting a BadRequestException")
    public void testCreateInvalidRecipeLongName(MockedTestContext testContext) {
        testContext
                .given(RecipeTestDto.class)
                .withName(getLongNameGenerator().stringGenerator(101))
                .whenException(recipeTestClient.createV4(), BadRequestException.class, expectedMessage("The length of the recipe's name has to be in range" +
                        " of 5 to 100"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "an invalid recipe request with too long description",
            when = "create recipe",
            then = "getting a BadRequestException")
    public void testCreateInvalidRecipeLongDescription(MockedTestContext testContext) {
        testContext
                .given(RecipeTestDto.class)
                .withDescription(getLongNameGenerator().stringGenerator(1001))
                .whenException(recipeTestClient.createV4(), BadRequestException.class, expectedMessage("size must be between 0 and 1000"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a deleted valid recipe",
            when = "list recipes",
            then = "recipe is not present")
    public void testCreateDeleteValidAndList(MockedTestContext testContext) {
        String recipeName = resourcePropertyProvider().getName();
        testContext
                .given(RecipeTestDto.class)
                .withName(recipeName)
                .when(recipeTestClient.createV4())
                .when(recipeTestClient.deleteV4())
                .then(recipeAuditGrpcServiceAssertion::create)
                .then(recipeAuditGrpcServiceAssertion::delete)
                .when(recipeTestClient.listV4());

        testContext
                .given(RecipeTestDto.class)
                .when((tc, recipeTestDto, cc) -> assertRecipesEmpty(tc, recipeTestDto, cc, recipeName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a valid recipe",
            when = "list recipes",
            then = "recipe is present")
    public void testCreateValidAndList(MockedTestContext testContext) {
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
