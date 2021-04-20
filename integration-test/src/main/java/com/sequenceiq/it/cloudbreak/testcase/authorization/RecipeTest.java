package com.sequenceiq.it.cloudbreak.testcase.authorization;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.expectedMessage;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.who;
import static com.sequenceiq.it.cloudbreak.util.AuthorizationTestUtil.datahubRecipePattern;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.actor.CloudbreakActor;
import com.sequenceiq.it.cloudbreak.client.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class RecipeTest extends AbstractIntegrationTest {

    @Inject
    private RecipeTestClient recipeTestClient;

    @Inject
    private CloudbreakActor cloudbreakActor;

    @Override
    protected void setupTest(TestContext testContext) {
        useRealUmsUser(testContext, AuthUserKeys.ACCOUNT_ADMIN);
        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_B);
        useRealUmsUser(testContext, AuthUserKeys.ZERO_RIGHTS);
        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_A);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a valid recipe",
            when = "a valid recipe create request sent",
            then = "only owner of the recipe and account admin should be able to describe or delete it")
    public void testRecipeActions(TestContext testContext) {
        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_A);
        testContext.given(RecipeTestDto.class)
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
                                .withWho(cloudbreakActor.useRealUmsUser(AuthUserKeys.ENV_CREATOR_B)))
                .whenException(recipeTestClient.getV4(), ForbiddenException.class,
                        expectedMessage("Doesn't have 'environments/useSharedResource' right on recipe " +
                                datahubRecipePattern(testContext.get(RecipeTestDto.class).getName()))
                                .withWho(cloudbreakActor.useRealUmsUser(AuthUserKeys.ZERO_RIGHTS)))
                .whenException(recipeTestClient.deleteV4(), ForbiddenException.class,
                        expectedMessage("Doesn't have 'environments/deleteRecipe' right on recipe " +
                                datahubRecipePattern(testContext.get(RecipeTestDto.class).getName()))
                                .withWho(cloudbreakActor.useRealUmsUser(AuthUserKeys.ENV_CREATOR_B)))
                .whenException(recipeTestClient.deleteV4(), ForbiddenException.class,
                        expectedMessage("Doesn't have 'environments/deleteRecipe' right on recipe " +
                                datahubRecipePattern(testContext.get(RecipeTestDto.class).getName()))
                                .withWho(cloudbreakActor.useRealUmsUser(AuthUserKeys.ZERO_RIGHTS)))
                .when(recipeTestClient.deleteV4(), who(cloudbreakActor.useRealUmsUser(AuthUserKeys.ENV_CREATOR_A)))
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
