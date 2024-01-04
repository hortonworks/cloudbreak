package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.PRE_SERVICE_DEPLOYMENT;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.PRE_TERMINATION;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.assertion.datalake.RecipeTestAssertion;
import com.sequenceiq.it.cloudbreak.assertion.salt.SaltHighStateDurationAssertions;
import com.sequenceiq.it.cloudbreak.client.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.util.RecipeUtil;
import com.sequenceiq.it.cloudbreak.util.ssh.SshJUtil;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class SdxRecipeTests extends PreconditionSdxE2ETest {

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private RecipeTestClient recipeTestClient;

    @Inject
    private SshJUtil sshJUtil;

    @Inject
    private RecipeUtil recipeUtil;

    @Inject
    private SaltHighStateDurationAssertions saltHighStateDurationAssertions;

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running Cloudbreak, and an SDX cluster in available state",
            when = "recipe called on MASTER host group",
            then = "SDX recipe executions should be successful"
    )
    public void testSDXPreClouderaManagerStartRecipe(TestContext testContext) {
        String recipeName = resourcePropertyProvider().getName();
        String preTerminationRecipeName = resourcePropertyProvider().getName();
        String filePath = "/pre-service-deployment";
        String fileName = "pre-service-deployment";
        String masterInstanceGroup = "master";
        testContext
                .given("preTermination", RecipeTestDto.class)
                    .withName(preTerminationRecipeName)
                    .withContent(recipeUtil.generatePreTerminationRecipeContentForE2E(applicationContext, preTerminationRecipeName))
                    .withRecipeType(PRE_TERMINATION)
                .when(recipeTestClient.createV4(), key("preTermination"))
                .given("preDeployment", RecipeTestDto.class)
                    .withName(recipeName)
                    .withContent(recipeUtil.generatePreDeploymentRecipeContent(applicationContext))
                    .withRecipeType(PRE_SERVICE_DEPLOYMENT)
                .when(recipeTestClient.createV4(), key("preDeployment"))
                .given(SdxTestDto.class)
                    .withRecipes(Set.of(recipeName, preTerminationRecipeName), masterInstanceGroup)
                    .withCloudStorage()
                .when(sdxTestClient.create())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .then(RecipeTestAssertion.validateFilesOnHost(List.of(MASTER.getName()), filePath, fileName, 1, sshJUtil))
                .then(saltHighStateDurationAssertions::saltHighStateDurationLimits)
                .when(sdxTestClient.delete())
                .await(SdxClusterStatusResponse.DELETED)
                .then((tc, testDto, client) -> verifyPreTerminationRecipe(tc, testDto, getBaseLocationForPreTermination(tc), preTerminationRecipeName))
                .validate();
    }

    private SdxTestDto verifyPreTerminationRecipe(TestContext testContext, SdxTestDto testDto, String cloudStorageBaseLocation, String recipeName) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageListContainer(cloudStorageBaseLocation, recipeName, false);
        return testDto;
    }
}
