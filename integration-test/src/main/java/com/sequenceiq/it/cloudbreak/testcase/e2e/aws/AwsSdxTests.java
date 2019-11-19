package com.sequenceiq.it.cloudbreak.testcase.e2e.aws;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.POST_CLOUDERA_MANAGER_START;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.IDBROKER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.it.cloudbreak.client.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.InstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.BasicSdxTests;
import com.sequenceiq.it.cloudbreak.util.amazonec2.AmazonEC2Util;
import com.sequenceiq.it.cloudbreak.util.ssh.SshJUtil;
import com.sequenceiq.it.cloudbreak.util.wait.WaitUtil;
import com.sequenceiq.it.util.ResourceUtil;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class AwsSdxTests extends BasicSdxTests {

    private static final String CREATE_FILE_RECIPE = "classpath:/recipes/post-install.sh";

    private Map<String, InstanceStatus> instancesDeletedOnProviderSide = new HashMap<String, InstanceStatus>() {{
        put(HostGroupType.MASTER.getName(), InstanceStatus.DELETED_ON_PROVIDER_SIDE);
        put(HostGroupType.IDBROKER.getName(), InstanceStatus.DELETED_ON_PROVIDER_SIDE);
    }};

    private Map<String, InstanceStatus> instancesStopped = new HashMap<String, InstanceStatus>() {{
        put(HostGroupType.MASTER.getName(), InstanceStatus.STOPPED);
        put(HostGroupType.IDBROKER.getName(), InstanceStatus.STOPPED);
    }};

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private RecipeTestClient recipeTestClient;

    @Inject
    private StackTestClient stackTestClient;

    @Inject
    private AmazonEC2Util amazonEC2Util;

    @Inject
    private WaitUtil waitUtil;

    @Inject
    private SshJUtil sshJUtil;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createEnvironmentForSdx(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running Cloudbreak, and an SDX cluster in available state",
            when = "recovery called on the IDBROKER and MASTER host group, where the EC2 instance had been terminated",
            then = "SDX recovery should be successful, the cluster should be up and running"
    )
    public void testSDXMultiRepairIDBRokerAndMasterWithTerminatedECInstances(TestContext testContext) {
        String sdx = resourcePropertyProvider().getName();

        List<String> actualMasterVolumeIds = new ArrayList<>();
        List<String> actualIDBrokerVolumeIds = new ArrayList<>();
        List<String> expectedMasterVolumeIds = new ArrayList<>();
        List<String> expectedIDBrokerVolumeIds = new ArrayList<>();

        testContext
                .given(sdx, SdxTestDto.class).withCloudStorage()
                .when(sdxTestClient.create(), key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .then((tc, testDto, client) -> {
                    return waitUtil.waitForSdxInstancesStatus(testDto, client, getSdxInstancesHealthyState());
                })
                .then((tc, testDto, client) -> {
                    expectedMasterVolumeIds.addAll(amazonEC2Util.listHostGroupVolumeIds(tc, testDto, client, HostGroupType.MASTER.getName()));
                    expectedIDBrokerVolumeIds.addAll(amazonEC2Util.listHostGroupVolumeIds(tc, testDto, client, HostGroupType.IDBROKER.getName()));
                    return testDto;
                })
                .then((tc, testDto, client) -> {
                    amazonEC2Util.deleteHostGroupInstances(tc, testDto, client, HostGroupType.MASTER.getName());
                    amazonEC2Util.deleteHostGroupInstances(tc, testDto, client, HostGroupType.IDBROKER.getName());
                    return testDto;
                })
                .then((tc, testDto, client) -> {
                    return waitUtil.waitForSdxInstancesStatus(testDto, client, instancesDeletedOnProviderSide);
                })
                .when(sdxTestClient.repair(), key(sdx))
                .await(SdxClusterStatusResponse.REPAIR_IN_PROGRESS, key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .then((tc, testDto, client) -> {
                    return waitUtil.waitForSdxInstancesStatus(testDto, client, getSdxInstancesHealthyState());
                })
                .then((tc, testDto, client) -> {
                    actualMasterVolumeIds.addAll(amazonEC2Util.listHostGroupVolumeIds(tc, testDto, client, HostGroupType.MASTER.getName()));
                    actualIDBrokerVolumeIds.addAll(amazonEC2Util.listHostGroupVolumeIds(tc, testDto, client, HostGroupType.IDBROKER.getName()));
                    return testDto;
                })
                .then((tc, testDto, client) -> {
                    return amazonEC2Util.compareVolumeIds(testDto, Stream.concat(actualMasterVolumeIds.stream(), actualIDBrokerVolumeIds.stream())
                            .collect(Collectors.toList()), Stream.concat(expectedMasterVolumeIds.stream(), expectedIDBrokerVolumeIds.stream())
                            .collect(Collectors.toList()));
                })
                .validate();
    }

    /**
     * This test case is disabled right now, because of [CB-3674 Can’t repair master when idbroker is stopped].
     *
     * @param testContext   Stores and shares test objects through test execution between individual test cases.
     * @throws IOException  Throws in case of recipe file stream cannot be written to or closed.
     *
     * The 'disabled' tag on method name and the '@Test(dataProvider = TEST_CONTEXT)' annotation should be restored in case of resume this test case.
     */
    @Description(
            given = "there is a running Cloudbreak, and an SDX cluster in available state",
            when = "recovery called on the IDBROKER and MASTER host group, where the EC2 instance had been stopped",
            then = "SDX recovery should be successful, the cluster should be up and running"
    )
    public void disabledTestSDXMultiRepairIDBRokerAndMasterWithRecipeFile(TestContext testContext) throws IOException {
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
                .given(cluster, ClusterTestDto.class).withValidateBlueprint(Boolean.FALSE).withClouderaManager(clouderaManager)
                .given(RecipeTestDto.class).withName(recipeName).withContent(generateRecipeContent())
                .withRecipeType(POST_CLOUDERA_MANAGER_START)
                .when(recipeTestClient.createV4())
                .given(masterInstanceGroup, InstanceGroupTestDto.class).withHostGroup(MASTER).withNodeCount(1).withRecipes(recipeName)
                .given(idbrokerInstanceGroup, InstanceGroupTestDto.class).withHostGroup(IDBROKER).withNodeCount(1).withRecipes(recipeName)
                .given(stack, StackTestDto.class).withCluster(cluster).withInstanceGroups(masterInstanceGroup, idbrokerInstanceGroup)
                .given(sdxInternal, SdxInternalTestDto.class).withStackRequest(stack, cluster)
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING)
                .then((tc, testDto, client) -> {
                    return waitUtil.waitForSdxInstancesStatus(testDto, client, getSdxInstancesHealthyState());
                })
                .then((tc, testDto, client) -> {
                    return sshJUtil.checkFilesOnHostByNameAndPath(testDto, client, List.of(HostGroupType.MASTER.getName(), HostGroupType.IDBROKER.getName()),
                            filePath, fileName, 1);
                })
                .then((tc, testDto, client) -> {
                    amazonEC2Util.stopHostGroupInstances(tc, testDto, client, HostGroupType.MASTER.getName());
                    amazonEC2Util.stopHostGroupInstances(tc, testDto, client, HostGroupType.IDBROKER.getName());
                    return testDto;
                })
                .then((tc, testDto, client) -> {
                    return waitUtil.waitForSdxInstancesStatus(testDto, client, instancesStopped);
                })
                .when(sdxTestClient.repairInternal(), key(sdxInternal))
                .await(SdxClusterStatusResponse.REPAIR_IN_PROGRESS, key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxInternal))
                .then((tc, testDto, client) -> {
                    return waitUtil.waitForSdxInstancesStatus(testDto, client, getSdxInstancesHealthyState());
                })
                .then((tc, testDto, client) -> {
                    return sshJUtil.checkFilesOnHostByNameAndPath(testDto, client, List.of(HostGroupType.MASTER.getName(), HostGroupType.IDBROKER.getName()),
                            filePath, fileName, 1);
                })
                .validate();
    }

    private String generateRecipeContent() throws IOException {
        String recipeContentFromFile = ResourceUtil.readResourceAsString(applicationContext, AwsSdxTests.CREATE_FILE_RECIPE);
        return Base64.encodeBase64String(recipeContentFromFile.getBytes());
    }
}
