package com.sequenceiq.it.cloudbreak.testcase.authorization;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.expectedMessage;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.who;
import static com.sequenceiq.it.cloudbreak.testcase.authorization.AuthUserKeys.ENV_CREATOR_A;
import static com.sequenceiq.it.cloudbreak.testcase.authorization.AuthUserKeys.ENV_CREATOR_B;
import static com.sequenceiq.it.cloudbreak.testcase.authorization.AuthUserKeys.ZERO_RIGHTS;
import static com.sequenceiq.it.cloudbreak.util.AuthorizationTestUtil.datahubRecipePattern;

import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.client.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.config.user.TestUserSelectors;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class RecipeTest extends AbstractIntegrationTest {

    @Inject
    private RecipeTestClient recipeTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getTestUsers().setSelector(TestUserSelectors.UMS_ONLY);

        testContext.as(AuthUserKeys.ACCOUNT_ADMIN);
        testContext.as(AuthUserKeys.ENV_CREATOR_B);
        testContext.as(AuthUserKeys.ZERO_RIGHTS);
        testContext.as(ENV_CREATOR_A);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a valid recipe",
            when = "a valid recipe create request sent",
            then = "only owner of the recipe and account admin should be able to describe or delete it")
    public void testRecipeActions(TestContext testContext) {
        CloudbreakUser envCreatorB = testContext.getTestUsers().getUserByLabel(ENV_CREATOR_B);
        CloudbreakUser envCreatorA = testContext.getTestUsers().getUserByLabel(ENV_CREATOR_A);
        CloudbreakUser zeroRights = testContext.getTestUsers().getUserByLabel(ZERO_RIGHTS);

        testContext.as(ENV_CREATOR_A)
                .given(RecipeTestDto.class)
                .when(recipeTestClient.createV4())
                .when(recipeTestClient.getV4())
                .then((context, dto, client) -> {
                    Assertions.assertThat(dto.getResponse().getName())
                            .isEqualTo(context.get(RecipeTestDto.class).getName());
                    return dto;
                })
                .when(recipeTestClient.listV4())
                .then((context, dto, client) -> {
                    Assertions.assertThat(dto.getSimpleResponses().getResponses()).isNotEmpty();
                    return dto;
                })
                .whenException(recipeTestClient.getV4(), ForbiddenException.class,
                        expectedMessage("Doesn't have 'environments/useSharedResource' right on recipe " +
                                datahubRecipePattern(testContext.get(RecipeTestDto.class).getName()))
                                .withWho(envCreatorB))
                .whenException(recipeTestClient.getV4(), ForbiddenException.class,
                        expectedMessage("Doesn't have 'environments/useSharedResource' right on recipe " +
                                datahubRecipePattern(testContext.get(RecipeTestDto.class).getName()))
                                .withWho(zeroRights))
                .whenException(recipeTestClient.deleteV4(), ForbiddenException.class,
                        expectedMessage("Doesn't have 'environments/deleteRecipe' right on recipe " +
                                datahubRecipePattern(testContext.get(RecipeTestDto.class).getName()))
                                .withWho(envCreatorB))
                .whenException(recipeTestClient.deleteV4(), ForbiddenException.class,
                        expectedMessage("Doesn't have 'environments/deleteRecipe' right on recipe " +
                                datahubRecipePattern(testContext.get(RecipeTestDto.class).getName()))
                                .withWho(zeroRights))
                .when(recipeTestClient.deleteV4(), who(envCreatorA))
                .when(recipeTestClient.listV4())
                .then((context, dto, client) -> {
                    Assertions.assertThat(
                            dto.getSimpleResponses().getResponses()
                                    .stream()
                                    .anyMatch(response -> response.getName().equals(dto.getName()))).isFalse();
                    return dto;
                })
                .validate();
    }

}
