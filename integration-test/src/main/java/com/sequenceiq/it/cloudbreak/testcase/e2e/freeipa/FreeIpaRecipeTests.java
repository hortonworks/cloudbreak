package com.sequenceiq.it.cloudbreak.testcase.e2e.freeipa;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.POST_SERVICE_DEPLOYMENT;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.PRE_SERVICE_DEPLOYMENT;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.expectedMessage;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.waitForFlow;

import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.it.cloudbreak.assertion.freeipa.RecipeTestAssertion;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUpscaleTestDto;
import com.sequenceiq.it.cloudbreak.dto.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.RecipeUtil;
import com.sequenceiq.it.cloudbreak.util.ssh.SshJUtil;

public class FreeIpaRecipeTests extends AbstractE2ETest {

    protected static final String PRE_RECIPE_FILEPATH = "/pre-service-deployment";

    protected static final String PRE_RECIPE_FILENAME = "pre-service-deployment";

    protected static final String POST_INSTALL_FILEPATH = "/post-service-deployment";

    protected static final String POST_INSTALL_FILENAME = "post-service-deployment";

    protected static final int INSTANCEGROUP_COUNT = 1;

    protected static final int INSTANCE_COUNT_BY_GROUP = 2;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private RecipeTestClient recipeTestClient;

    @Inject
    private RecipeUtil recipeUtil;

    @Inject
    private SshJUtil sshJUtil;

    public static String recipePattern(String recipePath) {
        return String.format("Required number [(]1[)] of files are NOT available at '%s' on '.*' instance!", recipePath);
    }

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        initializeDefaultBlueprints(testContext);
        initializeAzureMarketplaceTermsPolicy(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "a default environment is available with no freeIPA",
            when = "creating a freeIPA for the environment",
                and = "freeipa should be up and running then attach recipes to the freeIPA",
            then = "attached recipes should be available and executed on the freeIPA")
    public void testAttachFreeIpaRecipes(TestContext testContext) {
        String preRecipeName = testContext
                .given("pre1", RecipeTestDto.class)
                    .withName(resourcePropertyProvider().getName())
                    .withContent(recipeUtil.generatePreDeploymentRecipeContent(applicationContext))
                    .withRecipeType(PRE_SERVICE_DEPLOYMENT)
                .when(recipeTestClient.createV4(), key("pre1")).getResponse().getName();
        String postInstallRecipeName = testContext
                .given("post1", RecipeTestDto.class)
                    .withName(resourcePropertyProvider().getName())
                    .withContent(recipeUtil.generatePostDeploymentRecipeContent(applicationContext))
                    .withRecipeType(POST_SERVICE_DEPLOYMENT)
                .when(recipeTestClient.createV4(), key("post1")).getResponse().getName();

        setUpEnvironmentTestDto(testContext, Boolean.TRUE, INSTANCE_COUNT_BY_GROUP)
                .when(getEnvironmentTestClient().create())
                .awaitForCreationFlow()
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .await(Status.AVAILABLE)
                .awaitForHealthyInstances()
                .when(freeIpaTestClient.attachRecipes(List.of(preRecipeName, postInstallRecipeName)))
                .when(freeIpaTestClient.repair(InstanceMetadataType.GATEWAY_PRIMARY))
                .await(Status.UPDATE_IN_PROGRESS, waitForFlow().withWaitForFlow(Boolean.FALSE))
                .await(Status.AVAILABLE)
                .awaitForHealthyInstances()
                .then(RecipeTestAssertion.validateFilesOnFreeIpa(PRE_RECIPE_FILEPATH, PRE_RECIPE_FILENAME, 1, sshJUtil))
                .then(RecipeTestAssertion.validateFilesOnFreeIpa(POST_INSTALL_FILEPATH, POST_INSTALL_FILENAME, 1, sshJUtil))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "a default environment is available with no freeIPA",
            when = "creating a freeIPA for the environment with recipes",
                and = "freeipa should be up and running furthermore recipes should be available and executed on the freeIPA",
            then = "detached recipes should not be available and executed on the new freeIPA instances")
    public void testDetachFreeIpaRecipes(TestContext testContext) {
        String preRecipeName = testContext
                .given("pre2", RecipeTestDto.class)
                    .withName(resourcePropertyProvider().getName())
                    .withContent(recipeUtil.generatePreDeploymentRecipeContent(applicationContext))
                    .withRecipeType(PRE_SERVICE_DEPLOYMENT)
                .when(recipeTestClient.createV4(), key("pre2")).getResponse().getName();
        String postInstallRecipeName = testContext
                .given("post2", RecipeTestDto.class)
                    .withName(resourcePropertyProvider().getName())
                    .withContent(recipeUtil.generatePostDeploymentRecipeContent(applicationContext))
                    .withRecipeType(POST_SERVICE_DEPLOYMENT)
                .when(recipeTestClient.createV4(), key("post2")).getResponse().getName();

        setUpEnvironmentTestDto(testContext, Boolean.TRUE, 1)
                .withFreeIpaRecipe(Set.of(preRecipeName, postInstallRecipeName))
                .when(getEnvironmentTestClient().create())
                .awaitForCreationFlow()
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .await(Status.AVAILABLE)
                .awaitForHealthyInstances()
                .then(RecipeTestAssertion.validateFilesOnFreeIpa(PRE_RECIPE_FILEPATH, PRE_RECIPE_FILENAME, 1, sshJUtil))
                .then(RecipeTestAssertion.validateFilesOnFreeIpa(POST_INSTALL_FILEPATH, POST_INSTALL_FILENAME, 1, sshJUtil))
                .when(freeIpaTestClient.detachRecipes(List.of(preRecipeName, postInstallRecipeName)))
                .given(FreeIpaUpscaleTestDto.class)
                    .withAvailabilityType(AvailabilityType.TWO_NODE_BASED)
                .when(freeIpaTestClient.upscale())
                .await(Status.AVAILABLE)
                .given(FreeIpaTestDto.class)
                .awaitForHealthyInstances()
                .thenException(RecipeTestAssertion.validateFilesOnFreeIpa(InstanceMetadataType.GATEWAY, PRE_RECIPE_FILEPATH, PRE_RECIPE_FILENAME,
                                1, sshJUtil), TestFailException.class,
                        expectedMessage(recipePattern(PRE_RECIPE_FILEPATH)))
                .thenException(RecipeTestAssertion.validateFilesOnFreeIpa(InstanceMetadataType.GATEWAY, POST_INSTALL_FILEPATH, POST_INSTALL_FILENAME,
                                1, sshJUtil), TestFailException.class,
                        expectedMessage(recipePattern(POST_INSTALL_FILEPATH)))
                .validate();
    }
}
