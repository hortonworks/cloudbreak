package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.IDBROKER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.it.cloudbreak.client.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;
import com.sequenceiq.it.cloudbreak.util.ssh.SshJUtil;
import com.sequenceiq.it.cloudbreak.util.wait.WaitUtil;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class SdxRepairTests extends PreconditionSdxE2ETest {

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
                .awaitForFlow(key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .then((tc, testDto, client) -> {
                    waitUtil.waitForSdxInstanceStatus(testDto.getResponse().getName(), tc, getSdxInstancesHealthyState());
                    return testDto;
                })
                .then((tc, testDto, client) -> {
                    List<String> instancesToDelete = sdxUtil.getInstanceIds(testDto, client, MASTER.getName());
                    instancesToDelete.addAll(sdxUtil.getInstanceIds(testDto, client, IDBROKER.getName()));
                    expectedVolumeIds.addAll(getCloudFunctionality(tc).listInstanceVolumeIds(instancesToDelete));
                    getCloudFunctionality(tc).deleteInstances(instancesToDelete);
                    return testDto;
                })
                .then((tc, testDto, client) -> {
                    waitUtil.waitForSdxInstanceStatus(testDto.getResponse().getName(), tc, getSdxInstancesDeletedOnProviderSideState());
                    return testDto;
                })
                .when(sdxTestClient.repair(), key(sdx))
                .await(SdxClusterStatusResponse.REPAIR_IN_PROGRESS, key(sdx))
                .awaitForFlow(key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .then((tc, testDto, client) -> {
                    waitUtil.waitForSdxInstanceStatus(testDto.getResponse().getName(), tc, getSdxInstancesHealthyState());
                    return testDto;
                })
                .then((tc, testDto, client) -> {
                    List<String> instanceIds = sdxUtil.getInstanceIds(testDto, client, MASTER.getName());
                    instanceIds.addAll(sdxUtil.getInstanceIds(testDto, client, IDBROKER.getName()));
                    actualVolumeIds.addAll(getCloudFunctionality(tc).listInstanceVolumeIds(instanceIds));
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
                .awaitForFlow(key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .then((tc, testDto, client) -> {
                    waitUtil.waitForSdxInstanceStatus(testDto.getResponse().getName(), tc, getSdxInstancesHealthyState());
                    return testDto;
                })
                .then((tc, testDto, client) -> {
                    List<String> instancesToStop = sdxUtil.getInstanceIds(testDto, client, IDBROKER.getName());
                    expectedIDBrokerVolumeIds.addAll(getCloudFunctionality(tc).listInstanceVolumeIds(instancesToStop));
                    getCloudFunctionality(tc).stopInstances(instancesToStop);
                    return testDto;
                })
                .then((tc, testDto, client) -> {
                    waitUtil.waitForSdxInstanceStatus(testDto.getResponse().getName(), tc, Map.of(IDBROKER.getName(), InstanceStatus.STOPPED));
                    return testDto;
                })
                .when(sdxTestClient.repair(), key(sdx))
                .await(SdxClusterStatusResponse.REPAIR_IN_PROGRESS, key(sdx))
                .awaitForFlow(key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .then((tc, testDto, client) -> {
                    waitUtil.waitForSdxInstanceStatus(testDto.getResponse().getName(), tc, getSdxInstancesHealthyState());
                    return testDto;
                })
                .then((tc, testDto, client) -> {
                    List<String> instanceIds = sdxUtil.getInstanceIds(testDto, client, IDBROKER.getName());
                    actualIDBrokerVolumeIds.addAll(getCloudFunctionality(tc).listInstanceVolumeIds(instanceIds));
                    return testDto;
                })
                .then((tc, testDto, client) -> {
                    return compareVolumeIdsAfterRepair(testDto, new ArrayList<>(actualIDBrokerVolumeIds),
                            new ArrayList<>(expectedIDBrokerVolumeIds));
                })
                .then((tc, testDto, client) -> {
                    getCloudFunctionality(tc).cloudStorageListContainerDataLake(getBaseLocation(testDto));
                    return testDto;
                })
                .then((tc, testDto, client) -> {
                    getCloudFunctionality(tc).cloudStorageListContainerFreeIPA(getBaseLocation(testDto));
                    return testDto;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running Cloudbreak, and an SDX cluster in available state",
            when = "recovery called on the MASTER host group, where the EC2 instance had been stopped",
            then = "SDX recovery should be successful, the cluster should be up and running"
    )
    public void testSDXRepairMasterWithStoppedEC2Instance(TestContext testContext) {
        String sdx = resourcePropertyProvider().getName();

        List<String> actualMasterVolumeIds = new ArrayList<>();
        List<String> expectedMasterVolumeIds = new ArrayList<>();

        testContext
                .given(sdx, SdxTestDto.class).withCloudStorage()
                .when(sdxTestClient.create(), key(sdx))
                .awaitForFlow(key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .then((tc, testDto, client) -> {
                    waitUtil.waitForSdxInstanceStatus(testDto.getResponse().getName(), tc, getSdxInstancesHealthyState());
                    return testDto;
                })
                .then((tc, testDto, client) -> {
                    List<String> instancesToStop = sdxUtil.getInstanceIds(testDto, client, MASTER.getName());
                    expectedMasterVolumeIds.addAll(getCloudFunctionality(tc).listInstanceVolumeIds(instancesToStop));
                    getCloudFunctionality(tc).stopInstances(instancesToStop);
                    return testDto;
                })
                .then((tc, testDto, client) -> {
                    waitUtil.waitForSdxInstanceStatus(testDto.getResponse().getName(), tc, Map.of(MASTER.getName(), InstanceStatus.STOPPED));
                    return testDto;
                })
                .when(sdxTestClient.repair(), key(sdx))
                .await(SdxClusterStatusResponse.REPAIR_IN_PROGRESS, key(sdx))
                .awaitForFlow(key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .then((tc, testDto, client) -> {
                    waitUtil.waitForSdxInstanceStatus(testDto.getResponse().getName(), tc, getSdxInstancesHealthyState());
                    return testDto;
                })
                .then((tc, testDto, client) -> {
                    List<String> instanceIds = sdxUtil.getInstanceIds(testDto, client, MASTER.getName());
                    actualMasterVolumeIds.addAll(getCloudFunctionality(tc).listInstanceVolumeIds(instanceIds));
                    return testDto;
                })
                .then((tc, testDto, client) -> {
                    return compareVolumeIdsAfterRepair(testDto, new ArrayList<>(actualMasterVolumeIds),
                            new ArrayList<>(expectedMasterVolumeIds));
                })
                .then((tc, testDto, client) -> {
                    getCloudFunctionality(tc).cloudStorageListContainerDataLake(getBaseLocation(testDto));
                    return testDto;
                })
                .then((tc, testDto, client) -> {
                    getCloudFunctionality(tc).cloudStorageListContainerFreeIPA(getBaseLocation(testDto));
                    return testDto;
                })
                .validate();
    }
}
