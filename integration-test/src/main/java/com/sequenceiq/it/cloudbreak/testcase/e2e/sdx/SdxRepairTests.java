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
import com.sequenceiq.it.cloudbreak.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.client.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.ImageSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.InstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;
import com.sequenceiq.it.cloudbreak.util.VolumeUtils;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshJClientActions;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

public class SdxRepairTests extends PreconditionSdxE2ETest {
    private static final String CRONTAB_LIST = "sudo crontab -l";

    private static final String MINIMAL_MEDIUM_DUTY_RUNTIME = "7.2.7";

    private static final String MINIMAL_ENTERPRISE_RUNTIME = "7.2.17";

    private static final String ENTERPRISE_BLUEPRINT_NAME = "%s - SDX Enterprise: Apache Hive Metastore, Apache Ranger, Apache Atlas";

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

    @Inject
    private ImageCatalogTestClient imageCatalogTestClient;

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
            given = "there is a Medium Duty SDX cluster in available state",
            when = "IDBroker and Gateway instances are going to be deleted on the provider side",
            then = "SDX repair should be done successfully, the cluster should be up and running"
    )
    public void testSDXMediumDutyRepair(TestContext testContext) {
        String sdx = resourcePropertyProvider().getName();

        List<String> actualVolumeIds = new ArrayList<>();
        List<String> expectedVolumeIds = new ArrayList<>();

        testContext
                .given(sdx, SdxTestDto.class)
                    .withCloudStorage()
                    .withRuntimeVersion(MINIMAL_MEDIUM_DUTY_RUNTIME)
                    .withClusterShape(SdxClusterShape.MEDIUM_DUTY_HA)
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

    // CB-21906 Disabled due to blueprint error ("Enable TLS/SSL for Kafka Broker does not have same value for the 4 Kafka Brokers")
//    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a Scalable SDX cluster in available state",
            when = "IDBroker and Gateway instances are going to be deleted on the provider side",
            then = "SDX repair should be done successfully, the cluster should be up and running"
    )
    public void testSDXEnterpriseRepair(TestContext testContext) {
        String sdxInternal = resourcePropertyProvider().getName();
        String cluster = resourcePropertyProvider().getName();
        String clouderaManager = resourcePropertyProvider().getName();
        String imageSettings = resourcePropertyProvider().getName();
        String stack = resourcePropertyProvider().getName();
        String imgCatalogKey = "test-img-catalog";
        String telemetry = "telemetry";

        SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NONE);
        sdxDatabaseRequest.setCreate(false);

        List<String> actualVolumeIds = new ArrayList<>();
        List<String> expectedVolumeIds = new ArrayList<>();

        testContext
                .given(imgCatalogKey, ImageCatalogTestDto.class)
                    .withName(imgCatalogKey)
                    .withUrl("https://cloudbreak-imagecatalog.s3.amazonaws.com/v3-test-cb-image-catalog.json")
                .when(imageCatalogTestClient.createIfNotExistV4())
                .given(imageSettings, ImageSettingsTestDto.class)
                    .withImageCatalog(imgCatalogKey)
                .given(clouderaManager, ClouderaManagerTestDto.class)
                .given(cluster, ClusterTestDto.class)
                    .withBlueprintName(String.format(ENTERPRISE_BLUEPRINT_NAME, MINIMAL_ENTERPRISE_RUNTIME))
                    .withValidateBlueprint(Boolean.FALSE)
                    .withClouderaManager(clouderaManager)
                .given(stack, StackTestDto.class)
                    .withCluster(cluster)
                    .withImageSettings(imageSettings)
                    .withInstanceGroupsEntity(InstanceGroupTestDto.sdxEnterpriseHostGroup(testContext))
                .given(telemetry, TelemetryTestDto.class)
                    .withLogging()
                    .withReportClusterLogs()
                .given(sdxInternal, SdxInternalTestDto.class)
                    .withDatabase(sdxDatabaseRequest)
                    .withCloudStorage(getCloudStorageRequest(testContext))
                    .withStackRequest(key(cluster), key(stack))
                    .withTelemetry(telemetry)
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING)
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
                        key(sdxInternal).withWaitForFlow(Boolean.FALSE).withIgnoredStatues(Set.of(SdxClusterStatusResponse.NODE_FAILURE)))
                .when(sdxTestClient.repairInternal("gateway", "idbroker"), key(sdxInternal))
                .await(SdxClusterStatusResponse.REPAIR_IN_PROGRESS,
                        key(sdxInternal).withWaitForFlow(Boolean.FALSE).withIgnoredStatues(Set.of(SdxClusterStatusResponse.CLUSTER_UNREACHABLE)))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxInternal))
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
