package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.POST_SERVICE_DEPLOYMENT;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.PRE_SERVICE_DEPLOYMENT;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.PRE_TERMINATION;

import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.it.cloudbreak.assertion.freeipa.FreeIpaKerberosTestAssertion;
import com.sequenceiq.it.cloudbreak.assertion.freeipa.FreeIpaLdapTestAssertion;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.mock.answer.DefaultResponseConfigure;
import com.sequenceiq.it.cloudbreak.dto.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.util.RecipeUtil;

public class FreeIpaCreationTest extends AbstractMockTest {

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private RecipeTestClient recipeTestClient;

    @Inject
    private RecipeUtil recipeUtil;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultEnvironment(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "environment is present",
            when = "calling a freeipa creation with attached recipes",
            then = "freeipa should be available with kerberos and ldap config, recipes should run")
    public void testCreateFreeIpaWithRecipes(MockedTestContext testContext) {
        String preRecipeName = resourcePropertyProvider().getName();
        String postInstallRecipeName = resourcePropertyProvider().getName();
        String preTerminationRecipeName = resourcePropertyProvider().getName();
        testContext
                .given(RecipeTestDto.class)
                    .withName(preRecipeName)
                    .withContent(recipeUtil.generatePreDeploymentRecipeContent(applicationContext))
                    .withRecipeType(PRE_SERVICE_DEPLOYMENT)
                .when(recipeTestClient.createV4())
                .given(RecipeTestDto.class)
                    .withName(postInstallRecipeName)
                    .withContent(recipeUtil.generatePostDeploymentRecipeContent(applicationContext))
                    .withRecipeType(POST_SERVICE_DEPLOYMENT)
                .when(recipeTestClient.createV4())
                .given(RecipeTestDto.class)
                    .withName(preTerminationRecipeName)
                    .withContent(recipeUtil.generatePreTerminationRecipeContent(applicationContext))
                    .withRecipeType(PRE_TERMINATION)
                .when(recipeTestClient.createV4())
                .given(FreeIpaTestDto.class)
                .withRecipes(Set.of(preRecipeName, postInstallRecipeName, preTerminationRecipeName))
                .when(freeIpaTestClient.create())
                .enableVerification()
                .awaitForCreationFlow()
                .then(FreeIpaKerberosTestAssertion.validate())
                .then(FreeIpaLdapTestAssertion.validate())
                .when(freeIpaTestClient.delete())
                .await(Status.DELETE_COMPLETED)
                .mockSalt().saltFileDistribute().post()
                    .parameters(Map.of("file", preRecipeName, "path", "/srv/salt/pre-recipes/scripts", "permissions", "0600"),
                            DefaultResponseConfigure.ParameterCheck.HAS_THESE_PARAMETERS)
                    .times(2).verify()
                .mockSalt().saltFileDistribute().post()
                .parameters(Map.of("file", postInstallRecipeName, "path", "/srv/salt/post-recipes/scripts", "permissions", "0600"),
                        DefaultResponseConfigure.ParameterCheck.HAS_THESE_PARAMETERS)
                .times(2).verify()
                .mockSalt().saltFileDistribute().post()
                .parameters(Map.of("file", preTerminationRecipeName, "path", "/srv/salt/pre-recipes/scripts", "permissions", "0600"),
                        DefaultResponseConfigure.ParameterCheck.HAS_THESE_PARAMETERS)
                .times(2).verify()
                .mockSalt().run().post().bodyContains(Set.of("fun=grains.append", "arg=recipes&arg=pre-service-deployment"), 1).times(1).verify()
                .mockSalt().run().post().bodyContains(Set.of("fun=grains.append", "arg=recipes&arg=post-service-deployment"), 1).times(1).verify()
                .mockSalt().run().post().bodyContains(Set.of("fun=grains.append", "arg=recipes&arg=pre-termination"), 1).times(1).verify()
                .mockSalt().run().post().bodyContains("state.highstate", 1).times(3).verify()
                .mockSalt().run().post().bodyContains(Set.of("state.apply", "arg=recipes.post-service-deployment"), 1).times(1).verify()
                .mockSalt().run().post().bodyContains(Set.of("state.apply", "arg=recipes.pre-termination"), 1).times(1).verify()
                .validate();
    }

}
