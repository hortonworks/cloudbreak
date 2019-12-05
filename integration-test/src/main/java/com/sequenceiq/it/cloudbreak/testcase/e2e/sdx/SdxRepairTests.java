package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.POST_CLOUDERA_MANAGER_START;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.IDBROKER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static java.lang.String.format;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
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
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.testcase.e2e.BasicSdxTests;
import com.sequenceiq.it.cloudbreak.testcase.e2e.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;
import com.sequenceiq.it.cloudbreak.util.ssh.SshJUtil;
import com.sequenceiq.it.cloudbreak.util.wait.WaitUtil;
import com.sequenceiq.it.util.ResourceUtil;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class SdxRepairTests extends BasicSdxTests {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxRepairTests.class);

    private static final String CREATE_FILE_RECIPE = "classpath:/recipes/post-install.sh";

    private final Map<String, InstanceStatus> instancesDeletedOnProviderSide = new HashMap<>() {{
        put(MASTER.getName(), InstanceStatus.DELETED_ON_PROVIDER_SIDE);
        put(IDBROKER.getName(), InstanceStatus.DELETED_ON_PROVIDER_SIDE);
    }};

    private final Map<String, InstanceStatus> instancesStopped = new HashMap<>() {{
        put(MASTER.getName(), InstanceStatus.STOPPED);
        put(IDBROKER.getName(), InstanceStatus.STOPPED);
    }};

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

    private CloudFunctionality cloudFunctionality;

    @Override
    protected void setupTest(TestContext testContext) {
        cloudFunctionality = testContext.getCloudProvider().getCloudFunctionality();
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
    public void testSDXMultiRepairIDBRokerAndMasterWithTerminatedEC2Instances(TestContext testContext) {
        String sdx = resourcePropertyProvider().getName();

        List<String> actualVolumeIds = new ArrayList<>();
        List<String> expectedVolumeIds = new ArrayList<>();

        testContext
                .given(sdx, SdxTestDto.class).withCloudStorage()
                .when(sdxTestClient.create(), key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .then((tc, testDto, client) -> waitUtil.waitForSdxInstancesStatus(testDto, client, getSdxInstancesHealthyState()))
                .then((tc, testDto, client) -> {
                    List<String> instancesToDelete = sdxUtil.getInstanceIds(testDto, client, MASTER.getName());
                    instancesToDelete.addAll(sdxUtil.getInstanceIds(testDto, client, IDBROKER.getName()));
                    expectedVolumeIds.addAll(cloudFunctionality.listInstanceVolumeIds(instancesToDelete));
                    cloudFunctionality.deleteInstances(instancesToDelete);
                    return testDto;
                })
                .then((tc, testDto, client) -> waitUtil.waitForSdxInstancesStatus(testDto, client, instancesDeletedOnProviderSide))
                .when(sdxTestClient.repair(), key(sdx))
                .await(SdxClusterStatusResponse.REPAIR_IN_PROGRESS, key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .then((tc, testDto, client) -> waitUtil.waitForSdxInstancesStatus(testDto, client, getSdxInstancesHealthyState()))
                .then((tc, testDto, client) -> {
                    List<String> instanceIds = sdxUtil.getInstanceIds(testDto, client, MASTER.getName());
                    instanceIds.addAll(sdxUtil.getInstanceIds(testDto, client, IDBROKER.getName()));
                    actualVolumeIds.addAll(cloudFunctionality.listInstanceVolumeIds(instanceIds));
                    return testDto;
                })
                .then((tc, testDto, client) -> compareVolumeIdsAfterRepair(testDto, actualVolumeIds, expectedVolumeIds))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running Cloudbreak, and an SDX cluster in available state",
            when = "recovery called on the IDBROKER host group, where the EC2 instance had been stopped",
            then = "SDX recovery should be successful, the cluster should be up and running"
    )
    public void testSDXRepairIDBRokerWithStoppedEC2Instance(TestContext testContext) {
        String sdx = resourcePropertyProvider().getName();

        List<String> actualIDBrokerVolumeIds = new ArrayList<>();
        List<String> expectedIDBrokerVolumeIds = new ArrayList<>();

        testContext
                .given(sdx, SdxTestDto.class).withCloudStorage()
                .when(sdxTestClient.create(), key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .then((tc, testDto, client) -> {
                    return waitUtil.waitForSdxInstancesStatus(testDto, client, getSdxInstancesHealthyState());
                })
                .then((tc, testDto, client) -> {
                    List<String> instancesToStop = sdxUtil.getInstanceIds(testDto, client, IDBROKER.getName());
                    expectedIDBrokerVolumeIds.addAll(cloudFunctionality.listInstanceVolumeIds(instancesToStop));
                    cloudFunctionality.stopInstances(instancesToStop);
                    return testDto;
                })
                .then((tc, testDto, client) -> {
                    return waitUtil.waitForSdxInstanceStatus(testDto, client, IDBROKER.getName(), InstanceStatus.SERVICES_UNHEALTHY);
                })
                .when(sdxTestClient.repair(), key(sdx))
                .await(SdxClusterStatusResponse.REPAIR_IN_PROGRESS, key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .then((tc, testDto, client) -> {
                    return waitUtil.waitForSdxInstancesStatus(testDto, client, getSdxInstancesHealthyState());
                })
                .then((tc, testDto, client) -> {
                    List<String> instanceIds = sdxUtil.getInstanceIds(testDto, client, IDBROKER.getName());
                    actualIDBrokerVolumeIds.addAll(cloudFunctionality.listInstanceVolumeIds(instanceIds));
                    return testDto;
                })
                .then((tc, testDto, client) -> {
                    return compareVolumeIdsAfterRepair(testDto, new ArrayList<>(actualIDBrokerVolumeIds),
                            new ArrayList<>(expectedIDBrokerVolumeIds));
                })
                .validate();
    }

    /**
     * This test case is disabled right now, because of [CB-4176 [Repair] Data Lake Cluster repair fails for master node when stopped from AWS].
     *
     * @param testContext   Stores and shares test objects through test execution between individual test cases.
     *
     * The 'disabled' tag on method name and the '@Test(dataProvider = TEST_CONTEXT)' annotation should be restored in case of resume this test case.
     */
    @Description(
            given = "there is a running Cloudbreak, and an SDX cluster in available state",
            when = "recovery called on the MASTER host group, where the EC2 instance had been stopped",
            then = "SDX recovery should be successful, the cluster should be up and running"
    )
    public void disbaledTestSDXRepairMasterWithStoppedEC2Instance(TestContext testContext) {
        String sdx = resourcePropertyProvider().getName();

        List<String> actualMasterVolumeIds = new ArrayList<>();
        List<String> expectedMasterVolumeIds = new ArrayList<>();

        testContext
                .given(sdx, SdxTestDto.class).withCloudStorage()
                .when(sdxTestClient.create(), key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .then((tc, testDto, client) -> {
                    return waitUtil.waitForSdxInstancesStatus(testDto, client, getSdxInstancesHealthyState());
                })
                .then((tc, testDto, client) -> {
                    List<String> instancesToStop = sdxUtil.getInstanceIds(testDto, client, MASTER.getName());
                    expectedMasterVolumeIds.addAll(cloudFunctionality.listInstanceVolumeIds(instancesToStop));
                    cloudFunctionality.stopInstances(instancesToStop);
                    return testDto;
                })
                .then((tc, testDto, client) -> {
                    return waitUtil.waitForSdxInstanceStatus(testDto, client, MASTER.getName(), InstanceStatus.STOPPED);
                })
                .when(sdxTestClient.repair(), key(sdx))
                .await(SdxClusterStatusResponse.REPAIR_IN_PROGRESS, key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .then((tc, testDto, client) -> {
                    return waitUtil.waitForSdxInstancesStatus(testDto, client, getSdxInstancesHealthyState());
                })
                .then((tc, testDto, client) -> {
                    List<String> instanceIds = sdxUtil.getInstanceIds(testDto, client, MASTER.getName());
                    actualMasterVolumeIds.addAll(cloudFunctionality.listInstanceVolumeIds(instanceIds));
                    return testDto;
                })
                .then((tc, testDto, client) -> {
                    return compareVolumeIdsAfterRepair(testDto, new ArrayList<>(actualMasterVolumeIds),
                            new ArrayList<>(expectedMasterVolumeIds));
                })
                .validate();
    }

    /**
     * This test case is disabled right now, because of [CB-3674 Can’t repair master when idbroker is stopped].
     *
     * @param testContext Stores and shares test objects through test execution between individual test cases.
     * @throws IOException Throws in case of recipe file stream cannot be written to or closed.
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
                    return sshJUtil.checkFilesOnHostByNameAndPath(testDto, client, List.of(MASTER.getName(), IDBROKER.getName()),
                            filePath, fileName, 1);
                })
                .then((tc, testDto, client) -> {
                    List<String> instanceIdsToStop = sdxUtil.getInstanceIds(testDto, client, MASTER.getName());
                    instanceIdsToStop.addAll(sdxUtil.getInstanceIds(testDto, client, IDBROKER.getName()));
                    cloudFunctionality.stopInstances(instanceIdsToStop);
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
                    return sshJUtil.checkFilesOnHostByNameAndPath(testDto, client, List.of(MASTER.getName(), IDBROKER.getName()),
                            filePath, fileName, 1);
                })
                .validate();
    }

    private String generateRecipeContent() throws IOException {
        String recipeContentFromFile = ResourceUtil.readResourceAsString(applicationContext, CREATE_FILE_RECIPE);
        return Base64.encodeBase64String(recipeContentFromFile.getBytes());
    }

    private SdxTestDto compareVolumeIdsAfterRepair(SdxTestDto sdxTestDto, List<String> actualVolumeIds, List<String> expectedVolumeIds) {
        actualVolumeIds.sort(Comparator.naturalOrder());
        expectedVolumeIds.sort(Comparator.naturalOrder());

        if (!actualVolumeIds.equals(expectedVolumeIds)) {
            LOGGER.error("Host Group does not have the desired volume IDs!");
            actualVolumeIds.forEach(volumeid -> Log.log(LOGGER, format(" Actual volume ID: %s ", volumeid)));
            expectedVolumeIds.forEach(volumeId -> Log.log(LOGGER, format(" Desired volume ID: %s ", volumeId)));
            throw new TestFailException("Host Group does not have the desired volume IDs!");
        } else {
            actualVolumeIds.forEach(volumeId -> Log.log(LOGGER, format(" Before and after SDX repair volume IDs are equal [%s]. ", volumeId)));
        }
        return sdxTestDto;
    }
}
