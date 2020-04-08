package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.POST_CLOUDERA_MANAGER_START;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.IDBROKER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.sequenceiq.it.cloudbreak.client.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.InstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;
import com.sequenceiq.it.cloudbreak.util.ssh.SshJUtil;
import com.sequenceiq.it.cloudbreak.util.wait.WaitUtil;
import com.sequenceiq.it.util.ResourceUtil;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class InternalSdxRepairWithRecipeTest extends PreconditionSdxE2ETest {

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private RecipeTestClient recipeTestClient;

    @Inject
    private StackTestClient stackTestClient;

    @Inject
    private WaitUtil waitUtil;

    @Inject
    private SshJUtil sshJUtil;

    @Inject
    private SdxUtil sdxUtil;

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running Cloudbreak, and an SDX cluster in available state",
            when = "recovery called on the IDBROKER and MASTER host group, where the EC2 instance had been stopped",
            then = "SDX recovery should be successful, the cluster should be up and running"
    )
    public void testSDXMultiRepairIDBRokerAndMasterWithRecipeFile(TestContext testContext) throws IOException {
        String sdxInternal = resourcePropertyProvider().getName();
        String cluster = resourcePropertyProvider().getName();
        String clouderaManager = resourcePropertyProvider().getName();
        String recipeName = resourcePropertyProvider().getName();
        String stack = resourcePropertyProvider().getName();
        String filePath = "/post-install";
        String fileName = "post-install";
        String masterInstanceGroup = "master";
        String idbrokerInstanceGroup = "idbroker";

        testContext
                .given(clouderaManager, ClouderaManagerTestDto.class)
                .given(cluster, ClusterTestDto.class).withBlueprintName(getDefaultSDXBlueprintName()).withValidateBlueprint(Boolean.FALSE)
                .withClouderaManager(clouderaManager)
                .given(RecipeTestDto.class).withName(recipeName).withContent(generateRecipeContent())
                .withRecipeType(POST_CLOUDERA_MANAGER_START)
                .when(recipeTestClient.createV4())
                .given(masterInstanceGroup, InstanceGroupTestDto.class).withHostGroup(MASTER).withNodeCount(1).withRecipes(recipeName)
                .given(idbrokerInstanceGroup, InstanceGroupTestDto.class).withHostGroup(IDBROKER).withNodeCount(1).withRecipes(recipeName)
                .given(stack, StackTestDto.class).withCluster(cluster).withInstanceGroups(masterInstanceGroup, idbrokerInstanceGroup)
                .given(sdxInternal, SdxInternalTestDto.class)
                .withStackRequest(key(cluster), key(stack))
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .awaitForFlow(key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING)
                .then((tc, testDto, client) -> {
                    waitUtil.waitForSdxInstanceStatus(testDto.getResponse().getName(), tc, getSdxInstancesHealthyState());
                    return testDto;
                })
                .then((tc, testDto, client) -> {
                    return sshJUtil.checkFilesOnHostByNameAndPath(testDto, client, List.of(MASTER.getName(), IDBROKER.getName()),
                            filePath, fileName, 1);
                })
                .then((tc, testDto, client) -> {
                    List<String> instanceIdsToStop = sdxUtil.getInstanceIds(testDto, client, MASTER.getName());
                    instanceIdsToStop.addAll(sdxUtil.getInstanceIds(testDto, client, IDBROKER.getName()));
                    getCloudFunctionality(tc).stopInstances(instanceIdsToStop);
                    return testDto;
                })
                .then((tc, testDto, client) -> {
                    waitUtil.waitForSdxInstanceStatus(testDto.getResponse().getName(), tc, getSdxInstancesStoppedState());
                    return testDto;
                })
                .when(sdxTestClient.repairInternal(), key(sdxInternal))
                .await(SdxClusterStatusResponse.REPAIR_IN_PROGRESS, key(sdxInternal))
                .awaitForFlow(key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxInternal))
                .then((tc, testDto, client) -> {
                    waitUtil.waitForSdxInstanceStatus(testDto.getResponse().getName(), tc, getSdxInstancesHealthyState());
                    return testDto;
                })
                .then((tc, testDto, client) -> {
                    return sshJUtil.checkFilesOnHostByNameAndPath(testDto, client, List.of(MASTER.getName(), IDBROKER.getName()),
                            filePath, fileName, 1);
                })
                .validate();
    }

    private String generateRecipeContent() throws IOException {
        String recipeContentFromFile = ResourceUtil.readResourceAsString(applicationContext, getRecipePath());
        return Base64.encodeBase64String(recipeContentFromFile.getBytes());
    }
}
