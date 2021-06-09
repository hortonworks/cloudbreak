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
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;
import com.sequenceiq.it.cloudbreak.util.VolumeUtils;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class SdxRepairTests extends PreconditionSdxE2ETest {

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private RecipeTestClient recipeTestClient;

    @Inject
    private StackTestClient stackTestClient;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private SdxUtil sdxUtil;

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
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
                .awaitForInstance(getSdxInstancesHealthyState())
                .then((tc, testDto, client) -> {
                    List<String> instancesToDelete = sdxUtil.getInstanceIds(testDto, client, MASTER.getName());
                    instancesToDelete.addAll(sdxUtil.getInstanceIds(testDto, client, IDBROKER.getName()));
                    expectedVolumeIds.addAll(getCloudFunctionality(tc).listInstanceVolumeIds(instancesToDelete));
                    getCloudFunctionality(tc).deleteInstances(instancesToDelete);
                    return testDto;
                })
                .awaitForInstance(getSdxInstancesDeletedOnProviderSideState())
                .when(sdxTestClient.repair(), key(sdx))
                .await(SdxClusterStatusResponse.REPAIR_IN_PROGRESS, key(sdx).withWaitForFlow(false))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .awaitForInstance(getSdxInstancesHealthyState())
                .then((tc, testDto, client) -> {
                    List<String> instanceIds = sdxUtil.getInstanceIds(testDto, client, MASTER.getName());
                    instanceIds.addAll(sdxUtil.getInstanceIds(testDto, client, IDBROKER.getName()));
                    actualVolumeIds.addAll(getCloudFunctionality(tc).listInstanceVolumeIds(instanceIds));
                    return testDto;
                })
                .then((tc, testDto, client) -> VolumeUtils.compareVolumeIdsAfterRepair(testDto, actualVolumeIds, expectedVolumeIds))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT, enabled = false)
    @Description(
            given = "there is a running Cloudbreak, and an SDX cluster in available state",
            when = "recovery called on the MASTER and then the IDBROKER host group, where the EC2 instance had been stopped",
            then = "SDX recovery should be successful, the cluster should be up and running"
    )
    public void testSDXRepairMasterAndIDBRokerWithStoppedEC2Instance(TestContext testContext) {
        String sdx = resourcePropertyProvider().getName();

        DescribeFreeIpaResponse describeFreeIpaResponse = testContext.given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .getResponse();

        SdxTestDto sdxTestDto = testContext
                .given(sdx, SdxTestDto.class).withCloudStorage()
                .when(sdxTestClient.create(), key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .awaitForInstance(getSdxInstancesHealthyState());

        repair(sdxTestDto, sdx, MASTER);
        repair(sdxTestDto, sdx, IDBROKER);

        sdxTestDto
                .then((tc, testDto, client) -> {
                    getCloudFunctionality(tc).cloudStorageListContainerDataLake(getBaseLocation(testDto),
                            testDto.getResponse().getName(), testDto.getResponse().getStackCrn());
                    return testDto;
                })
                .then((tc, testDto, client) -> {
                    getCloudFunctionality(tc).cloudStorageListContainerFreeIpa(getBaseLocation(testDto),
                            describeFreeIpaResponse.getName(), describeFreeIpaResponse.getCrn());
                    return testDto;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT, enabled = false)
    @Description(
            given = "there is a running Cloudbreak, and an SDX medium Duty cluster in available state",
            when = "",
            then = "SDX creation should be successful, the cluster should be up and running"
    )
    public void testSDXMediumDutyCreation(TestContext testContext) {
        String sdx = resourcePropertyProvider().getName();

        testContext.given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .getResponse();

        testContext
                .given(sdx, SdxTestDto.class).withCloudStorage()
                .withClusterShape(SdxClusterShape.MEDIUM_DUTY_HA)
                .when(sdxTestClient.create(), key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .awaitForInstance(getSdxInstancesHealthyState());
        // once fix HA salt been implemented CB-12410, we will add the repair part
    }

    private void repair(SdxTestDto sdxTestDto, String sdx, HostGroupType hostGroupType) {
        List<String> expectedVolumeIds = new ArrayList<>();
        List<String> actualVolumeIds = new ArrayList<>();

        sdxTestDto
                .then((tc, testDto, client) -> {
                    List<String> instancesToStop = sdxUtil.getInstanceIds(testDto, client, hostGroupType.getName());
                    expectedVolumeIds.addAll(getCloudFunctionality(tc).listInstanceVolumeIds(instancesToStop));
                    getCloudFunctionality(tc).stopInstances(instancesToStop);
                    return testDto;
                })
                .awaitForInstance(Map.of(hostGroupType.getName(), InstanceStatus.STOPPED))
                .when(sdxTestClient.repair(), key(sdx))
                .await(SdxClusterStatusResponse.REPAIR_IN_PROGRESS, key(sdx).withWaitForFlow(Boolean.FALSE))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .awaitForInstance(getSdxInstancesHealthyState())
                .then((tc, testDto, client) -> {
                    List<String> instanceIds = sdxUtil.getInstanceIds(testDto, client, hostGroupType.getName());
                    actualVolumeIds.addAll(getCloudFunctionality(tc).listInstanceVolumeIds(instanceIds));
                    return testDto;
                })
                .then((tc, testDto, client) -> VolumeUtils.compareVolumeIdsAfterRepair(testDto,
                        new ArrayList<>(actualVolumeIds),
                        new ArrayList<>(expectedVolumeIds)));
    }
}
