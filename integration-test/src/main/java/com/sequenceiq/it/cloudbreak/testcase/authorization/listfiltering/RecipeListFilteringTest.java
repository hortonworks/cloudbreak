package com.sequenceiq.it.cloudbreak.testcase.authorization.listfiltering;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeViewV4Response;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;
import com.sequenceiq.it.cloudbreak.testcase.authorization.AuthUserKeys;
import com.sequenceiq.it.cloudbreak.util.ResourceCreator;

public class RecipeListFilteringTest extends AbstractIntegrationTest {

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private ResourceCreator resourceCreator;

    @Override
    protected void setupTest(TestContext testContext) {
        useRealUmsUser(testContext, AuthUserKeys.USER_ACCOUNT_ADMIN);
        useRealUmsUser(testContext, AuthUserKeys.USER_ENV_CREATOR_A);
        useRealUmsUser(testContext, AuthUserKeys.USER_ENV_CREATOR_B);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there are recipes",
            when = "users share with each other",
            then = "they see the other's recipe in the list")
    public void testRecipeListFiltering(TestContext testContext) {
        useRealUmsUser(testContext, AuthUserKeys.USER_ENV_CREATOR_A);
        RecipeTestDto recipeA = resourceCreator.createDefaultRecipe(testContext);

        useRealUmsUser(testContext, AuthUserKeys.USER_ENV_CREATOR_B);
        RecipeTestDto recipeB = resourceCreator.createNewRecipe(testContext);

        assertUserSeesAll(testContext, AuthUserKeys.USER_ENV_CREATOR_A, recipeA.getName());
        assertUserSeesAll(testContext, AuthUserKeys.USER_ENV_CREATOR_B, recipeB.getName());
        assertUserSeesAll(testContext, AuthUserKeys.USER_ACCOUNT_ADMIN, recipeA.getName(), recipeB.getName());

        assertUserDoesNotSeeAnyOf(testContext, AuthUserKeys.USER_ENV_CREATOR_A, recipeB.getName());
        assertUserDoesNotSeeAnyOf(testContext, AuthUserKeys.USER_ENV_CREATOR_B, recipeA.getName());

        testContext.given(UmsTestDto.class)
                .assignTarget(RecipeTestDto.class.getSimpleName())
                .withSharedResourceUser()
                .when(environmentTestClient.assignResourceRole(AuthUserKeys.USER_ENV_CREATOR_B))
                .validate();

        assertUserSeesAll(testContext, AuthUserKeys.USER_ENV_CREATOR_A, recipeA.getName());
        assertUserSeesAll(testContext, AuthUserKeys.USER_ENV_CREATOR_B, recipeA.getName(), recipeB.getName());
        assertUserSeesAll(testContext, AuthUserKeys.USER_ACCOUNT_ADMIN, recipeA.getName(), recipeB.getName());

        testContext.given(UmsTestDto.class)
                .assignTarget(recipeB.getName())
                .withSharedResourceUser()
                .when(environmentTestClient.assignResourceRole(AuthUserKeys.USER_ENV_CREATOR_A))
                .validate();

        assertUserSeesAll(testContext, AuthUserKeys.USER_ENV_CREATOR_A, recipeA.getName(), recipeB.getName());
        assertUserSeesAll(testContext, AuthUserKeys.USER_ENV_CREATOR_B, recipeA.getName(), recipeB.getName());
        assertUserSeesAll(testContext, AuthUserKeys.USER_ACCOUNT_ADMIN, recipeA.getName(), recipeB.getName());

        useRealUmsUser(testContext, AuthUserKeys.USER_ACCOUNT_ADMIN);
    }

    private void assertUserDoesNotSeeAnyOf(TestContext testContext, String user, String... names) {
        useRealUmsUser(testContext, user);
        Assertions.assertThat(testContext.given(RecipeTestDto.class).getAll(testContext.getMicroserviceClient(CloudbreakClient.class))
                .stream()
                .map(RecipeViewV4Response::getName)
                .collect(Collectors.toList()))
                .doesNotContainAnyElementsOf(Arrays.asList(names));
    }

    private void assertUserSeesAll(TestContext testContext, String user, String... names) {
        useRealUmsUser(testContext, user);
        List<String> visibleNames = testContext.given(RecipeTestDto.class).getAll(testContext.getMicroserviceClient(CloudbreakClient.class))
                .stream()
                .map(RecipeViewV4Response::getName)
                .collect(Collectors.toList());
        Assertions.assertThat(visibleNames).containsAll(Arrays.asList(names));
    }
}
