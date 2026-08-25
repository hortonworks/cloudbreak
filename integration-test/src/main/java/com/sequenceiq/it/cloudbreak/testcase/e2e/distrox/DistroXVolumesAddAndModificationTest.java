package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.util.CollectionUtils;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.volume.DatabaseVolumeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.volume.VolumeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.resource.ResourceV4Response;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpDiskType;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.it.cloudbreak.assertion.selinux.SELinuxAssertions;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupsBuilder;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2EWithReusableResourcesTest;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.DistroxUtil;
import com.sequenceiq.it.cloudbreak.util.InstanceHostDiskAssertions;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class DistroXVolumesAddAndModificationTest extends AbstractE2EWithReusableResourcesTest {

    private static final Map<String, String> SDX_TAGS = Map.of("sdxTagKey", "sdxTagValue");

    private static final Map<String, String> DX_TAGS = Map.of("distroxTagKey", "distroxTagValue");

    private static final String TEST_INSTANCE_GROUP = "coordinator";

    private static final String DB_INSTANCE_GROUP = "master";

    private static final int UPDATE_SIZE = 500;

    private static final int ADD_DISK_SIZE = 200;

    private static final long NUM_DISK_TO_ADD = 2;

    private static final int ROOT_UPDATE_SIZE = 310;

    private static final int DB_UPDATE_SIZE = 300;

    private static final String ROOT_VOLUMES = "rootVolumes";

    private static final String ADDITIONAL_VOLUMES = "additionalVolumes";

    private static final String DB_VOLUMES = "dbVolumes";

    private static final String GCP_LOCAL_SSD_DEVICE_PREFIX = "local-";

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private DistroxUtil distroxUtil;

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    @Inject
    private SELinuxAssertions selinuxAssertions;

    @Inject
    private InstanceHostDiskAssertions instanceHostDiskAssertions;

    @Override
    protected void setupClass(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultEnvironment(testContext);
        createAndWaitDatalake(testContext);
    }

    private void createAndWaitDatalake(TestContext testContext) {
        testContext
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs()
                .given(SdxInternalTestDto.class)
                .withTelemetry("telemetry")
                .addTags(SDX_TAGS)
                .withSeLinuxSecurity(SeLinux.ENFORCING.name())
                .withCloudStorage(getCloudStorageRequest(testContext))
                .when(sdxTestClient.createInternal())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> selinuxAssertions.validateAll(tc, testDto, false));
    }

    private void createAndWaitDataHub(TestContext testContext) {
        testContext
                .given("dx", DistroXTestDto.class)
                .withTemplate(commonClusterManagerProperties.getDataMartDistroXBlueprintNameForCurrentRuntime())
                .withSeLinuxSecurity(SeLinux.ENFORCING.name())
                .withInstanceGroupsEntity(new DistroXInstanceGroupsBuilder(testContext)
                        .verticalScaleHostGroup()
                        .withStorageOptimizedInstancetype()
                        .withStorageOptimizedVolumeType()
                        .build())
                .addTags(DX_TAGS)
                .when(distroXTestClient.create(), RunningParameter.key("dx"))
                .await(STACK_AVAILABLE, RunningParameter.key("dx"))
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> selinuxAssertions.validateAll(tc, testDto, false));
    }

    @Test(dataProvider = TEST_CONTEXT, timeOut = 9000000)
    @Description(
            given = "there is an available environment with a running datahub",
            when = "root disk modification is done",
            then = "root volume on datahubs must be modified to the new type and size"
    )
    public void testDistroXRootVolumeModification(TestContext testContext) {
        CloudPlatform cloudPlatform = testContext.getCloudPlatform();

        createAndWaitDataHub(testContext);
        testContext
                .given("dx", DistroXTestDto.class)
                .when(distroXTestClient.updateDisks(ROOT_UPDATE_SIZE, testContext.getCloudProvider().getModifyDiskVolumeType(), TEST_INSTANCE_GROUP,
                        DiskType.ROOT_DISK), RunningParameter.key("dx"))
                .await(STACK_AVAILABLE, RunningParameter.key("dx"))
                .awaitForHealthyInstances()
                .given("dx", DistroXTestDto.class)
                .when(distroXTestClient.getStackWithResources(), RunningParameter.key("dx"))
                .then((tc, testDto, client) -> {
                    validateRootDisks(testDto, tc, client, cloudPlatform);
                    return testDto;
                })
                .awaitForHealthyInstances()
                .useAlternativeServiceEndpointIfConfigured()
                .validate();
    }

    // Splitting up Root Volume modification and additional volumes modification, because in GCP additional volume modification requires start and stop of
    // entire cluster and is a long-running process in itself. Adding that to root volume modification will create time-out errors.
    @Test(dataProvider = TEST_CONTEXT, timeOut = 9000000)
    @Description(
            given = "there is an available environment with a running datahub",
            when = "additional disk modification is called, followed by database disk modification",
            then = "attached data volumes and the database volume on the datahub must be modified to the new type and size"
    )
    public void testDistroXAdditionalVolumesModification(TestContext testContext) {
        CloudPlatform cloudPlatform = testContext.getCloudPlatform();

        createAndWaitDataHub(testContext);
        testContext
                .given("dx", DistroXTestDto.class)
                .when(distroXTestClient.updateDisks(UPDATE_SIZE, testContext.getCloudProvider().getModifyDiskVolumeType(),
                        TEST_INSTANCE_GROUP, DiskType.ADDITIONAL_DISK), RunningParameter.key("dx"))
                .await(STACK_AVAILABLE, RunningParameter.key("dx"))
                .awaitForHealthyInstances()
                .given("dx", DistroXTestDto.class)
                .when(distroXTestClient.get(), RunningParameter.key("dx"))
                .then((tc, testDto, client) -> {
                    validateUpdatedDisks(testDto, tc, client, cloudPlatform);
                    return testDto;
                })
                .awaitForHealthyInstances()
                .given("dx", DistroXTestDto.class)
                .when(distroXTestClient.updateDisks(DB_UPDATE_SIZE, testContext.getCloudProvider().getModifyDiskVolumeType(),
                        DB_INSTANCE_GROUP, DiskType.DATABASE_DISK), RunningParameter.key("dx"))
                .await(STACK_AVAILABLE, RunningParameter.key("dx"))
                .awaitForHealthyInstances()
                .given("dx", DistroXTestDto.class)
                .when(distroXTestClient.get(), RunningParameter.key("dx"))
                .then((tc, testDto, client) -> {
                    validateUpdatedDbDisk(testDto, tc, client, cloudPlatform);
                    return testDto;
                })
                .then((tc, testDto, client) -> selinuxAssertions.validateAll(tc, testDto, false))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT, timeOut = 9000000)
    @Description(
            given = "there is an available environment with a running datahub",
            when = "delete disks is called on the Datahub's coordinator group and then add volumes is called",
            then = "attached EBS volumes on datahubs must be deleted and new volumes must be added, " +
                    "the new datahub should be up and running"
    )
    public void testDistroXDeleteAndAddVolumes(TestContext testContext) {
        CloudPlatform cloudPlatform = testContext.getCloudPlatform();

        createAndWaitDataHub(testContext);
        testContext
                .given("dx", DistroXTestDto.class)
                .when(distroXTestClient.deleteDisks(TEST_INSTANCE_GROUP), RunningParameter.key("dx"))
                .await(STACK_AVAILABLE, RunningParameter.key("dx"))
                .awaitForHealthyInstances()
                .given("dx", DistroXTestDto.class)
                .when(distroXTestClient.get(), RunningParameter.key("dx"))
                .then((tc, testDto, client) -> {
                    validateDeletedDisk(testDto, tc, client);
                    return testDto;
                })
                .awaitForHealthyInstances()
                .given("dx", DistroXTestDto.class)
                .when(distroXTestClient.addDisks(ADD_DISK_SIZE, testContext.getCloudProvider().getAddDiskVolumeType(), TEST_INSTANCE_GROUP, NUM_DISK_TO_ADD),
                        RunningParameter.key("dx"))
                .await(STACK_AVAILABLE, RunningParameter.key("dx"))
                .awaitForHealthyInstances()
                .given("dx", DistroXTestDto.class)
                .when(distroXTestClient.get(), RunningParameter.key("dx"))
                .then((tc, testDto, client) -> {
                    validateAddedDisks(testDto, tc, client, cloudPlatform);
                    return testDto;
                })
                .then((tc, testDto, client) -> selinuxAssertions.validateAll(tc, testDto, false))
                .validate();
    }

    private void validateUpdatedDisks(DistroXTestDto distroXTestDto, TestContext tc, CloudbreakClient client, CloudPlatform cloudPlatform) {
        String expectedVolumeType = tc.getCloudProvider().getModifyDiskVolumeType();

        List<String> attachedVolumes = getVolumesOnCloudProvider(distroXTestDto, tc, client, ADDITIONAL_VOLUMES);
        if (CollectionUtils.isEmpty(attachedVolumes)) {
            throw new TestFailException(String.format("Update Disk did not complete successfully on cloud provider for instances in group %s. " +
                    "Attached Volumes %s on cloud provider does not match with expected number of Volumes", TEST_INSTANCE_GROUP, attachedVolumes));
        }

        Set<VolumeV4Response> attachedVolumesWithGroup = getVolumes(distroXTestDto);
        if (CollectionUtils.isEmpty(attachedVolumesWithGroup)) {
            throw new TestFailException(String.format("Update Disk did not complete successfully for instances in group %s. " +
                    "Attached Volumes %s does not match with expected number of Volumes in CB", TEST_INSTANCE_GROUP, attachedVolumesWithGroup));

        }
        attachedVolumesWithGroup.forEach(vol -> {
            if (vol.getSize() != UPDATE_SIZE || (expectedVolumeType != null && !expectedVolumeType.equalsIgnoreCase(vol.getType()))) {
                throw new TestFailException(String.format("Update Disk did not complete successfully for instances in group %s in CB. " +
                                "Expected: [size: %s, type: %s], Actual: [size: %s, type: %s]",
                        TEST_INSTANCE_GROUP, UPDATE_SIZE, expectedVolumeType, vol.getSize(), vol.getType()));
            }
        });

        List<Volume> attachedVolumesAttributes = getCloudFunctionality(tc).describeVolumes(attachedVolumes);
        List<Volume> misalignedVolumes = new ArrayList<>();
        attachedVolumesAttributes.forEach(vol -> {
            if (vol.getSize() != UPDATE_SIZE || (expectedVolumeType != null && !expectedVolumeType.equalsIgnoreCase(vol.getType()))) {
                misalignedVolumes.add(vol);
            }
        });

        if (!misalignedVolumes.isEmpty()) {
            throw new TestFailException(String.format("Update Disk did not complete successfully for instances on cloud provider in group %s. " +
                            "Misaligned volumes: %s. Expected size: %s, Expected type: %s",
                    TEST_INSTANCE_GROUP, misalignedVolumes, UPDATE_SIZE, expectedVolumeType));
        }

        instanceHostDiskAssertions.assertMountPointsAtLeastProvisionedSize(distroXTestDto.getResponse().getInstanceGroups(), TEST_INSTANCE_GROUP,
            hadoopFsMountsExpectedGiB(attachedVolumesWithGroup, UPDATE_SIZE),
            "additional volume resize (df/lsblk)");
    }

    private void validateDeletedDisk(DistroXTestDto distroXTestDto, TestContext tc, CloudbreakClient client) {
        List<String> attachedVolumes = getVolumesOnCloudProvider(distroXTestDto, tc, client, ADDITIONAL_VOLUMES).stream()
                .filter(volumeId -> !volumeId.startsWith(GCP_LOCAL_SSD_DEVICE_PREFIX))
                .toList();
        if (!CollectionUtils.isEmpty(attachedVolumes)) {
            throw new TestFailException(String.format("Disk Delete did not complete successfully for instances in group %s. " +
                            "volumes %s are still attached on cloud provider",
                    TEST_INSTANCE_GROUP, attachedVolumes));

        }
        Set<VolumeV4Response> stillAttachedVolumes = getVolumes(distroXTestDto);
        if (!CollectionUtils.isEmpty(stillAttachedVolumes)) {
            throw new TestFailException(String.format("Disk Delete did not complete successfully for instances in group %s. " +
                            "There are still volumes %s attached in CB",
                    TEST_INSTANCE_GROUP, stillAttachedVolumes));

        }
    }

    private void validateAddedDisks(DistroXTestDto distroXTestDto, TestContext tc, CloudbreakClient client, CloudPlatform cloudPlatform) {
        String expectedVolumeType = tc.getCloudProvider().getAddDiskVolumeType();

        List<String> attachedVolumes = getVolumesOnCloudProvider(distroXTestDto, tc, client, ADDITIONAL_VOLUMES).stream()
                .filter(volumeId -> !volumeId.startsWith(GCP_LOCAL_SSD_DEVICE_PREFIX))
                .toList();
        if (attachedVolumes.size() != NUM_DISK_TO_ADD) {
            throw new TestFailException(String.format("Add Disk did not complete successfully on cloud provider for instances in group %s. " +
                    "Attached Volumes %s on cloud provider does not match with expected number of Volumes", TEST_INSTANCE_GROUP, attachedVolumes));
        }

        Set<VolumeV4Response> attachedVolumesWithGroup = getVolumes(distroXTestDto);
        if (attachedVolumesWithGroup.stream().mapToInt(VolumeV4Response::getCount).sum() != NUM_DISK_TO_ADD) {
            throw new TestFailException(String.format("Add Disk did not complete successfully for instances in group %s. " +
                    "Attached Volumes %s does not match with expected number of Volumes in CB", TEST_INSTANCE_GROUP, attachedVolumesWithGroup));

        }
        attachedVolumesWithGroup.forEach(vol -> {
            if (vol.getSize() != ADD_DISK_SIZE || (expectedVolumeType != null && !expectedVolumeType.equalsIgnoreCase(vol.getType()))) {
                throw new TestFailException(String.format("Add Disk did not complete successfully for instances in group %s in CB. " +
                                "Expected: [size: %s, type: %s], Actual: [size: %s, type: %s]",
                        TEST_INSTANCE_GROUP, ADD_DISK_SIZE, expectedVolumeType, vol.getSize(), vol.getType()));
            }
        });

        List<Volume> attachedVolumesAttributes = getCloudFunctionality(tc).describeVolumes(attachedVolumes);
        List<Volume> misalignedVolumes = new ArrayList<>();
        attachedVolumesAttributes.forEach(vol -> {
            if (vol.getSize() != ADD_DISK_SIZE || (expectedVolumeType != null && !expectedVolumeType.equalsIgnoreCase(vol.getType()))) {
                misalignedVolumes.add(vol);
            }
        });

        if (!misalignedVolumes.isEmpty()) {
            throw new TestFailException(String.format("Add Disk did not complete successfully for instances on cloud provider in group %s. " +
                            "Misaligned volumes: %s. Expected size: %s, Expected type: %s",
                    TEST_INSTANCE_GROUP, misalignedVolumes, ADD_DISK_SIZE, expectedVolumeType));
        }

        instanceHostDiskAssertions.assertMountPointsAtLeastProvisionedSize(distroXTestDto.getResponse().getInstanceGroups(), TEST_INSTANCE_GROUP,
            hadoopFsMountsExpectedGiB(attachedVolumesWithGroup, ADD_DISK_SIZE),
            "added volume sizing (df/lsblk)");
    }

    private List<String> getVolumesOnCloudProvider(DistroXTestDto distroXTestDto, TestContext tc, CloudbreakClient client, String volumeType) {
        CloudFunctionality cloudFunctionality = getCloudFunctionality(tc);
        switch (volumeType) {
            case ROOT_VOLUMES -> {
                List<String> updatedInstances = distroxUtil.getInstanceIds(distroXTestDto, client, TEST_INSTANCE_GROUP);
                return cloudFunctionality.listInstancesRootVolumeIds(distroXTestDto.getName(), updatedInstances);
            }
            case ADDITIONAL_VOLUMES -> {
                List<String> updatedInstances = distroxUtil.getInstanceIds(distroXTestDto, client, TEST_INSTANCE_GROUP);
                return cloudFunctionality.listInstancesVolumeIds(distroXTestDto.getName(), updatedInstances);
            }
            case DB_VOLUMES -> {
                List<String> updatedInstances = distroxUtil.getInstanceIds(distroXTestDto, client, DB_INSTANCE_GROUP);
                return cloudFunctionality.listInstancesVolumeIds(distroXTestDto.getName(), updatedInstances);
            }
            default -> throw new TestFailException("Invalid configuration, unexpected value: " + volumeType);
        }
    }

    private Set<VolumeV4Response> getVolumes(DistroXTestDto distroXTestDto) {
        StackV4Response stackV4Response = distroXTestDto.getResponse();
        InstanceGroupV4Response instanceGroup = stackV4Response.getInstanceGroups().stream().filter(ig -> ig.getName().equals(TEST_INSTANCE_GROUP))
                .findFirst().orElseThrow();
        Set<VolumeV4Response> attachedVolumesWithGroup = instanceGroup.getTemplate().getAttachedVolumes().stream()
                .filter(volumeV4Response -> volumeV4Response.getCount() > 0)
                .filter(volumeV4Response -> !GcpDiskType.LOCAL_SSD.value().equalsIgnoreCase(volumeV4Response.getType()))
                .collect(Collectors.toSet());
        return attachedVolumesWithGroup;
    }

    private DatabaseVolumeV4Response getDatabaseVolume(DistroXTestDto distroXTestDto) {
        StackV4Response stackV4Response = distroXTestDto.getResponse();
        InstanceGroupV4Response instanceGroup = stackV4Response.getInstanceGroups().stream().filter(ig -> ig.getName().equals(DB_INSTANCE_GROUP))
                .findFirst().orElseThrow();
        return instanceGroup.getTemplate().getDatabaseVolume();
    }

    private List<ResourceV4Response> getRootVolumes(DistroXTestDto distroXTestDto, TestContext tc) {
        StackV4Response stackV4Response = distroXTestDto.getResponse();
        return stackV4Response.getResources().stream()
                .filter(res -> res.getResourceType().equals(tc.getCloudProvider().getRootDiskResourceType())
                        && res.getInstanceGroup().equals(TEST_INSTANCE_GROUP))
                .toList();
    }

    protected CloudFunctionality getCloudFunctionality(TestContext testContext) {
        return testContext.getCloudProvider().getCloudFunctionality();
    }

    private void validateRootDisks(DistroXTestDto distroXTestDto, TestContext tc, CloudbreakClient client, CloudPlatform cloudPlatform) {
        String expectedVolumeType = tc.getCloudProvider().getModifyDiskVolumeType();

        List<String> rootVolumes = getVolumesOnCloudProvider(distroXTestDto, tc, client, ROOT_VOLUMES);
        if (CollectionUtils.isEmpty(rootVolumes)) {
            throw new TestFailException(String.format("Root volume is not present on instances on Cloud Provider for group %s",
                    TEST_INSTANCE_GROUP));
        }

        List<Volume> rootVolumesAttributes = getCloudFunctionality(tc).describeVolumes(rootVolumes);
        List<Volume> misalignedVolumes = new ArrayList<>();
        rootVolumesAttributes.forEach(vol -> {
            if (vol.getSize() != ROOT_UPDATE_SIZE || (expectedVolumeType != null && !expectedVolumeType.equalsIgnoreCase(vol.getType()))) {
                misalignedVolumes.add(vol);
            }
        });

        if (!misalignedVolumes.isEmpty()) {
            throw new TestFailException(String.format("Root Volume Modification did not complete successfully for instances on cloud provider in group %s. " +
                            "Misaligned volumes: %s. Expected size: %s, Expected type: %s",
                    TEST_INSTANCE_GROUP, misalignedVolumes, ROOT_UPDATE_SIZE, expectedVolumeType));
        }

        List<ResourceV4Response> rootVolumesInGroup = getRootVolumes(distroXTestDto, tc);
        if (CollectionUtils.isEmpty(rootVolumesInGroup)) {
            throw new TestFailException(String.format("Root volume is not present on instances in CB for group %s",
                    TEST_INSTANCE_GROUP));

        }
    }

    private void validateUpdatedDbDisk(DistroXTestDto distroXTestDto, TestContext tc, CloudbreakClient client, CloudPlatform cloudPlatform) {
        String expectedVolumeType = tc.getCloudProvider().getModifyDiskVolumeType();

        List<String> attachedVolumes = getVolumesOnCloudProvider(distroXTestDto, tc, client, DB_VOLUMES);
        if (CollectionUtils.isEmpty(attachedVolumes)) {
            throw new TestFailException(String.format("Update DB Disk did not complete successfully on cloud provider for instances in group %s. " +
                    "Attached Volumes %s on cloud provider does not match with expected number of Volumes", DB_INSTANCE_GROUP, attachedVolumes));
        }

        DatabaseVolumeV4Response databaseVolume = getDatabaseVolume(distroXTestDto);
        if (databaseVolume == null) {
            throw new TestFailException(String.format("Update DB Disk did not complete successfully for instances in group %s. " +
                    "DatabaseVolume %s does not match with expected number of Volumes in CB", DB_INSTANCE_GROUP, databaseVolume));

        }
        if (databaseVolume.getSize() != DB_UPDATE_SIZE || (expectedVolumeType != null && !expectedVolumeType.equalsIgnoreCase(databaseVolume.getType()))) {
            throw new TestFailException(String.format("Update DB Disk did not complete successfully for instances in group %s in CB. " +
                            "Expected: [size: %s, type: %s], Actual: [size: %s, type: %s]",
                    DB_INSTANCE_GROUP, DB_UPDATE_SIZE, expectedVolumeType, databaseVolume.getSize(), databaseVolume.getType()));
        }

        List<Volume> attachedVolumesAttributes = getCloudFunctionality(tc).describeVolumes(attachedVolumes);
        List<Volume> misalignedVolumes = new ArrayList<>();
        for (Volume vol : attachedVolumesAttributes) {
            if (vol.getVolumeUsageType() == CloudVolumeUsageType.DATABASE
                    && (vol.getSize() != DB_UPDATE_SIZE || (expectedVolumeType != null && !expectedVolumeType.equalsIgnoreCase(vol.getType())))) {
                misalignedVolumes.add(vol);
            }
        }

        if (!misalignedVolumes.isEmpty()) {
            throw new TestFailException(String.format("Update Disk did not complete successfully for instances on cloud provider in group %s. " +
                            "Misaligned volumes: %s. Expected size: %s, Expected type: %s",
                    DB_INSTANCE_GROUP, misalignedVolumes, DB_UPDATE_SIZE, expectedVolumeType));
        }

        instanceHostDiskAssertions.assertMountPointsAtLeastProvisionedSize(distroXTestDto.getResponse().getInstanceGroups(), DB_INSTANCE_GROUP,
            Map.of("/dbfs", DB_UPDATE_SIZE), "database volume resize (df/lsblk)");
    }

    private Map<String, Integer> hadoopFsMountsExpectedGiB(Set<VolumeV4Response> attachedVolumesWithGroup, int sizeGiB) {
        int volumeCount = attachedVolumesWithGroup.stream().mapToInt(VolumeV4Response::getCount).sum();
        Map<String, Integer> mounts = new LinkedHashMap<>();
        for (int i = 1; i <= volumeCount; i++) {
            mounts.put("/hadoopfs/fs" + i, sizeGiB);
        }
        return mounts;
    }
}