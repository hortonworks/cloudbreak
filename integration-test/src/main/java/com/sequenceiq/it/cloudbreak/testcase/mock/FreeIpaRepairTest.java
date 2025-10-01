package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.POST_SERVICE_DEPLOYMENT;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.PRE_SERVICE_DEPLOYMENT;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.PRE_TERMINATION;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.waitForFlow;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.it.cloudbreak.assertion.audit.FreeIpaAuditGrpcServiceAssertion;
import com.sequenceiq.it.cloudbreak.assertion.freeipa.FreeIpaListStructuredEventAssertions;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.mock.answer.DefaultResponseConfigure;
import com.sequenceiq.it.cloudbreak.dto.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.util.RecipeUtil;

/**
 * To run locally you might need to set the following for freeipa application:
 * freeipa.delayed.scale-sec=1
 */
public class FreeIpaRepairTest extends AbstractMockTest {

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private RecipeUtil recipeUtil;

    @Inject
    private RecipeTestClient recipeTestClient;

    @Inject
    private FreeIpaListStructuredEventAssertions freeIpaListStructuredEventAssertions;

    @Inject
    private FreeIpaAuditGrpcServiceAssertion freeIpaAuditGrpcServiceAssertion;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultEnvironment(testContext);
    }

    // NOTE: set -Dfreeipa.delayed.scale-sec=1 for freeipa application, without this it will time out.
    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "environment is present",
            when = "creating freeipa without recipes, then attaching some recipes and calling a freeipa repair on each instances",
            then = "freeipa should be available and attached recipes should run on replaced instances")
    public void testFreeIpaRepairWithAttachDetachRecipes(MockedTestContext testContext) {
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
                .withFreeIpaHa(1, 2)
                .when(freeIpaTestClient.create())
                .enableVerification()
                .await(Status.AVAILABLE)
                .when(freeIpaTestClient.attachRecipes(List.of(preRecipeName, postInstallRecipeName, preTerminationRecipeName)))
                .when(freeIpaTestClient.repair(InstanceMetadataType.GATEWAY_PRIMARY))
                .await(Status.UPDATE_IN_PROGRESS, waitForFlow().withWaitForFlow(Boolean.FALSE))
                .await(Status.AVAILABLE)
                .awaitForHealthyInstances()
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
                .mockSalt().run().post().bodyContains("state.highstate", 1).times(6).verify()
                .mockSalt().run().post().bodyContains(Set.of("state.apply", "arg=recipes.post-service-deployment"), 1).times(1).verify()
                .mockSalt().run().post().bodyContains(Set.of("state.apply", "arg=recipes.pre-termination"), 1).times(1).verify()
                .validate();
    }
}
