package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.PRE_CLOUDERA_MANAGER_START;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;

import java.util.List;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.assertion.datalake.RecipeTestAssertion;
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

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running Cloudbreak, and an SDX cluster in available state",
            when = "recipe called on MASTER host group",
            then = "SDX recipe executions should be successful"
    )
    public void testSDXPreClouderaManagerStartRecipe(TestContext testContext) {
        String recipeName = resourcePropertyProvider().getName();
        String filePath = "/pre-ambari";
        String fileName = "pre-ambari";
        String masterInstanceGroup = "master";
        testContext
                .given(RecipeTestDto.class)
                    .withName(recipeName)
                    .withContent(recipeUtil.generatePreCmStartRecipeContent(applicationContext))
                    .withRecipeType(PRE_CLOUDERA_MANAGER_START)
                .when(recipeTestClient.createV4())
                .given(SdxTestDto.class)
                    .withRecipe(recipeName, masterInstanceGroup)
                    .withCloudStorage()
                .when(sdxTestClient.create())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .then(RecipeTestAssertion.validateFilesOnHost(List.of(MASTER.getName()), filePath, fileName, 1, null, null, sshJUtil))
                .validate();
    }
}
