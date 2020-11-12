package com.sequenceiq.it.cloudbreak.testcase.authorization;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.actor.Actor;
import com.sequenceiq.it.cloudbreak.client.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class RecipeTest extends AbstractIntegrationTest {

    @Inject
    private RecipeTestClient recipeTestClient;

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
                .when(recipeTestClient.getV4(), RunningParameter.who(Actor.useRealUmsUser(AuthUserKeys.ENV_CREATOR_B)))
                .expect(ForbiddenException.class,
                        RunningParameter.expectedMessage("You have insufficient rights to perform the following action[(]s[)]: " +
                                "'environments/useSharedResource'" +
                                ".*crn:cdp:datahub:.*").withKey("RecipeGetAction"))
                .when(recipeTestClient.getV4(), RunningParameter.who(Actor.useRealUmsUser(AuthUserKeys.ZERO_RIGHTS)))
                .expect(ForbiddenException.class,
                        RunningParameter.expectedMessage("You have insufficient rights to perform the following action[(]s[)]: " +
                                "'environments/useSharedResource'" +
                                ".*crn:cdp:datahub:.*").withKey("RecipeGetAction"))
                .when(recipeTestClient.deleteV4(), RunningParameter.who(Actor.useRealUmsUser(AuthUserKeys.ENV_CREATOR_B)))
                .expect(ForbiddenException.class,
                        RunningParameter.expectedMessage("You have insufficient rights to perform the following action[(]s[)]: 'environments/deleteRecipe'" +
                                ".*crn:cdp:datahub:.*").withKey("RecipeDeleteAction"))
                .when(recipeTestClient.deleteV4(), RunningParameter.who(Actor.useRealUmsUser(AuthUserKeys.ZERO_RIGHTS)))
                .expect(ForbiddenException.class,
                        RunningParameter.expectedMessage("You have insufficient rights to perform the following action[(]s[)]: 'environments/deleteRecipe'" +
                                ".*crn:cdp:datahub:.*").withKey("RecipeDeleteAction"))
                .when(recipeTestClient.deleteV4())
                .when(recipeTestClient.listV4())
                .then((context, dto, client) -> {
                    Assertions.assertThat(
                            dto.getSimpleResponses().getResponses()
                                    .stream()
                                    .filter(response -> response.getName()
                                            .equals(dto.getName())).findFirst().isPresent()).isFalse();
                    return dto;
                })
                .validate();
    }
}
