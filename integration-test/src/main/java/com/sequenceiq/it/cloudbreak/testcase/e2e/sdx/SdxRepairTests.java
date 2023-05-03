package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.IDBROKER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;
import com.sequenceiq.it.cloudbreak.util.VolumeUtils;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshJClientActions;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class SdxRepairTests extends PreconditionSdxE2ETest {
    private static final String CRONTAB_LIST = "sudo crontab -l";

    private static final String MINIMAL_MEDIUM_DUTY_RUNTIME = "7.2.7";

    private static final String MINIMAL_ENTERPRISE_RUNTIME = "7.2.17";

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

    @Inject
    private SshJClientActions sshJClientActions;

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
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> assertCronCreatedOnMasterNodesForUserHomeCreation(testDto))
                .then((tc, testDto, client) -> {
                    List<String> instancesToDelete = sdxUtil.getInstanceIds(testDto, client, MASTER.getName());
                    instancesToDelete.addAll(sdxUtil.getInstanceIds(testDto, client, IDBROKER.getName()));
                    expectedVolumeIds.addAll(getCloudFunctionality(tc).listInstanceVolumeIds(testDto.getName(), instancesToDelete));
                    getCloudFunctionality(tc).deleteInstances(testDto.getName(), instancesToDelete);
                    return testDto;
                })
                .awaitForDeletedInstancesOnProvider()
                .await(SdxClusterStatusResponse.DELETED_ON_PROVIDER_SIDE,
                        key(sdx).withWaitForFlow(Boolean.FALSE)
                                .withIgnoredStatues(Set.of(SdxClusterStatusResponse.NODE_FAILURE, SdxClusterStatusResponse.CLUSTER_UNREACHABLE)))
                .when(sdxTestClient.repair(MASTER.getName(), IDBROKER.getName()), key(sdx))
                .await(SdxClusterStatusResponse.REPAIR_IN_PROGRESS,
                        key(sdx).withWaitForFlow(Boolean.FALSE).withIgnoredStatues(Set.of(SdxClusterStatusResponse.DELETED_ON_PROVIDER_SIDE)))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> {
                    List<String> instanceIds = sdxUtil.getInstanceIds(testDto, client, MASTER.getName());
                    instanceIds.addAll(sdxUtil.getInstanceIds(testDto, client, IDBROKER.getName()));
                    actualVolumeIds.addAll(getCloudFunctionality(tc).listInstanceVolumeIds(testDto.getName(), instanceIds));
                    return testDto;
                })
                .then((tc, testDto, client) -> VolumeUtils.compareVolumeIdsAfterRepair(testDto, actualVolumeIds, expectedVolumeIds))
                .validate();
    }

    private SdxTestDto assertCronCreatedOnMasterNodesForUserHomeCreation(SdxTestDto testDto) {
        Map<String, Pair<Integer, String>> crontabListResultByIpsMap =
                sshJClientActions.executeSshCommandOnHost(testDto.getResponse().getStackV4Response().getInstanceGroups(),
                        List.of(MASTER.getName()), CRONTAB_LIST, false);
        Set<String> nodesWithoutCrontabForUserHomeCreation = crontabListResultByIpsMap.entrySet().stream()
                .filter(entry -> !entry.getValue().getValue().contains("createuserhome.sh"))
                .map(Entry::getKey)
                .collect(Collectors.toSet());
        if (!nodesWithoutCrontabForUserHomeCreation.isEmpty()) {
            throw new TestFailException("Missing crontab for user home creation for nodes: " + nodesWithoutCrontabForUserHomeCreation);
        }
        return testDto;
    }

    @Test(dataProvider = TEST_CONTEXT)
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
                .given(sdx, SdxTestDto.class)
                    .withCloudStorage(getCloudStorageRequest(testContext))
                .when(sdxTestClient.create(), key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .awaitForHealthyInstances();

        repair(sdxTestDto, sdx, MASTER.getName(), Set.of(SdxClusterStatusResponse.CLUSTER_UNREACHABLE));
        repair(sdxTestDto, sdx, IDBROKER.getName(), Set.of(SdxClusterStatusResponse.NODE_FAILURE));

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

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running Cloudbreak, and an SDX medium Duty cluster in available state",
            when = "repair is run",
            then = "SDX repair should be successful, the cluster should be up and running"
    )
    public void testSDXMediumDutyRepair(TestContext testContext) {
        repairHA(testContext, SdxClusterShape.MEDIUM_DUTY_HA, MINIMAL_MEDIUM_DUTY_RUNTIME);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running Cloudbreak, and a Scalable SDX cluster in available state",
            when = "repair is run",
            then = "SDX repair should be successful, the cluster should be up and running"
    )
    public void testSDXEnterpriseRepair(TestContext testContext) {
        repairHA(testContext, SdxClusterShape.ENTERPRISE, MINIMAL_ENTERPRISE_RUNTIME);
    }

    private void repairHA(TestContext testContext, SdxClusterShape sdxClusterShape, String runtime) {
        String sdx = resourcePropertyProvider().getName();

        List<String> actualVolumeIds = new ArrayList<>();
        List<String> expectedVolumeIds = new ArrayList<>();

        testContext
                .given(EnvironmentTestDto.class)
                .given(sdx, SdxTestDto.class).withCloudStorage()
                .withRuntimeVersion(runtime)
                .withClusterShape(sdxClusterShape)
                .when(sdxTestClient.create(), key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> {
                    List<String> instancesToDelete = sdxUtil.getInstanceIds(testDto, client, "gateway");
                    instancesToDelete.addAll(sdxUtil.getInstanceIds(testDto, client, "idbroker"));
                    expectedVolumeIds.addAll(getCloudFunctionality(tc).listInstanceVolumeIds(testDto.getName(), instancesToDelete));
                    getCloudFunctionality(tc).deleteInstances(testDto.getName(), instancesToDelete);
                    return testDto;
                })
                .awaitForHostGroups(List.of("gateway", "idbroker"), InstanceStatus.DELETED_ON_PROVIDER_SIDE)
                .await(SdxClusterStatusResponse.CLUSTER_UNREACHABLE,
                        key(sdx).withWaitForFlow(Boolean.FALSE).withIgnoredStatues(Set.of(SdxClusterStatusResponse.NODE_FAILURE)))
                .when(sdxTestClient.repair("gateway", "idbroker"), key(sdx))
                .await(SdxClusterStatusResponse.REPAIR_IN_PROGRESS,
                        key(sdx).withWaitForFlow(Boolean.FALSE).withIgnoredStatues(Set.of(SdxClusterStatusResponse.CLUSTER_UNREACHABLE)))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> {
                    List<String> instanceIds = sdxUtil.getInstanceIds(testDto, client, "gateway");
                    instanceIds.addAll(sdxUtil.getInstanceIds(testDto, client, "idbroker"));
                    actualVolumeIds.addAll(getCloudFunctionality(tc).listInstanceVolumeIds(testDto.getName(), instanceIds));
                    return testDto;
                })
                .then((tc, testDto, client) -> VolumeUtils.compareVolumeIdsAfterRepair(testDto, actualVolumeIds, expectedVolumeIds))
                .validate();
    }

    private void repair(SdxTestDto sdxTestDto, String sdx, String hostgroupName, Set<SdxClusterStatusResponse> ignoredFailedStatuses) {
        List<String> expectedVolumeIds = new ArrayList<>();
        List<String> actualVolumeIds = new ArrayList<>();

        sdxTestDto
                .then((tc, testDto, client) -> {
                    List<String> instancesToStop = sdxUtil.getInstanceIds(testDto, client, hostgroupName);
                    expectedVolumeIds.addAll(getCloudFunctionality(tc).listInstanceVolumeIds(testDto.getName(), instancesToStop));
                    getCloudFunctionality(tc).stopInstances(testDto.getName(), instancesToStop);
                    return testDto;
                })
                .awaitForHostGroups(List.of(hostgroupName), InstanceStatus.STOPPED)
                .when(sdxTestClient.repair(hostgroupName), key(sdx))
                .await(SdxClusterStatusResponse.REPAIR_IN_PROGRESS, key(sdx).withWaitForFlow(Boolean.FALSE)
                        .withIgnoredStatues(new HashSet<>(ignoredFailedStatuses)))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> {
                    List<String> instanceIds = sdxUtil.getInstanceIds(testDto, client, hostgroupName);
                    actualVolumeIds.addAll(getCloudFunctionality(tc).listInstanceVolumeIds(testDto.getName(), instanceIds));
                    return testDto;
                })
                .then((tc, testDto, client) -> VolumeUtils.compareVolumeIdsAfterRepair(testDto,
                        new ArrayList<>(actualVolumeIds),
                        new ArrayList<>(expectedVolumeIds)));
    }
}
