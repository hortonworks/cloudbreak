package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.util.CollectionUtils;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.volume.VolumeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.resource.ResourceV4Response;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.ResourceType;
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
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class DistroXVolumesAddAndModificationTest extends AbstractE2EWithReusableResourcesTest {

    private static final Map<CloudPlatform, ResourceType> PLATFORM_RESOURCE_TYPE_MAP = ImmutableMap.of(CloudPlatform.AWS, ResourceType.AWS_ROOT_DISK,
            CloudPlatform.AZURE, ResourceType.AZURE_DISK);

    private static final Map<String, String> SDX_TAGS = Map.of("sdxTagKey", "sdxTagValue");

    private static final Map<String, String> DX_TAGS = Map.of("distroxTagKey", "distroxTagValue");

    private static final String TEST_INSTANCE_GROUP = "coordinator";

    private static final int UPDATE_SIZE = 500;

    private static final int ADD_DISK_SIZE = 200;

    private static final long NUM_DISK_TO_ADD = 2;

    private static final int ROOT_UPDATE_SIZE = 310;

    private static final String AWS_DISK_TYPE = "gp2";

    private static final String AZURE_DISK_TYPE = "StandardSSD_LRS";

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

    @Override
    protected void setupClass(TestContext testContext) {
        assertNotSupportedCloudPlatform(CloudPlatform.GCP);
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
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
        CloudPlatform cloudPlatform = testContext.getCloudPlatform();
        String instanceType = CloudPlatform.AWS.equals(cloudPlatform) ? "m5d.2xlarge" : "Standard_D8s_v3";

        testContext
                .given("dx", DistroXTestDto.class)
                .withTemplate(commonClusterManagerProperties.getDataMartDistroXBlueprintNameForCurrentRuntime())
                .withSeLinuxSecurity(SeLinux.ENFORCING.name())
                .withInstanceGroupsEntity(new DistroXInstanceGroupsBuilder(testContext)
                        .verticalScaleHostGroup()
                        .withInstanceType(instanceType)
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
            when = "root disk modification is done first, then additional disk resize is called first",
            then = "attached EBS volumes on datahubs must be modified to the new type and size"
    )
    public void testDistroXVolumesModification(TestContext testContext) {
        CloudPlatform cloudPlatform = testContext.getCloudPlatform();

        createAndWaitDataHub(testContext);
        testContext
                .given("dx", DistroXTestDto.class)
                .when(distroXTestClient.updateDisks(ROOT_UPDATE_SIZE, getVolumeTypeForUpdatingDisks(cloudPlatform), TEST_INSTANCE_GROUP,
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
                .given("dx", DistroXTestDto.class)
                .when(distroXTestClient.updateDisks(UPDATE_SIZE, getVolumeTypeForUpdatingDisks(cloudPlatform),
                        TEST_INSTANCE_GROUP, DiskType.ADDITIONAL_DISK), RunningParameter.key("dx"))
                .await(STACK_AVAILABLE, RunningParameter.key("dx"))
                .awaitForHealthyInstances()
                .given("dx", DistroXTestDto.class)
                .when(distroXTestClient.get(), RunningParameter.key("dx"))
                .then((tc, testDto, client) -> {
                    validateUpdatedDisks(testDto, tc, client, cloudPlatform);
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
                .when(distroXTestClient.addDisks(ADD_DISK_SIZE, getVolumeTypeForAddingDisks(cloudPlatform), TEST_INSTANCE_GROUP, NUM_DISK_TO_ADD),
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
        String expectedVolumeType = getVolumeTypeForUpdatingDisks(cloudPlatform);

        List<String> attachedVolumes = getVolumesOnCloudProvider(distroXTestDto, tc, client, false);
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
                throw new TestFailException(String.format("Update Disk did not complete successfully for instances in group %s in CB",
                        TEST_INSTANCE_GROUP));
            }
        });

        List<Volume> attachedVolumesAttributes = getCloudFunctionality(tc).describeVolumes(attachedVolumes);
        attachedVolumesAttributes.forEach(vol -> {
            if (vol.getSize() != UPDATE_SIZE || (expectedVolumeType != null && !expectedVolumeType.equalsIgnoreCase(vol.getType()))) {
                throw new TestFailException(String.format("Update Disk did not complete successfully for instances on cloud provider in group %s",
                        TEST_INSTANCE_GROUP));
            }
        });

    }

    private void validateDeletedDisk(DistroXTestDto distroXTestDto, TestContext tc, CloudbreakClient client) {
        List<String> attachedVolumes = getVolumesOnCloudProvider(distroXTestDto, tc, client, false);
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
        String expectedVolumeType = getVolumeTypeForAddingDisks(cloudPlatform);

        List<String> attachedVolumes = getVolumesOnCloudProvider(distroXTestDto, tc, client, false);
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
                throw new TestFailException(String.format("Add Disk did not complete successfully for instances in group %s in CB",
                        TEST_INSTANCE_GROUP));
            }
        });

        List<Volume> attachedVolumesAttributes = getCloudFunctionality(tc).describeVolumes(attachedVolumes);
        attachedVolumesAttributes.forEach(vol -> {
            if (vol.getSize() != ADD_DISK_SIZE || (expectedVolumeType != null && !expectedVolumeType.equalsIgnoreCase(vol.getType()))) {
                throw new TestFailException(String.format("Add Disk did not complete successfully for instances on cloud provider in group %s",
                        TEST_INSTANCE_GROUP));
            }
        });

    }

    private String getVolumeTypeForAddingDisks(CloudPlatform cloudPlatform) {
        if (cloudPlatform == CloudPlatform.AWS) {
            return AWS_DISK_TYPE;
        } else if (cloudPlatform == CloudPlatform.AZURE) {
            return AZURE_DISK_TYPE;
        }
        return null;
    }

    private String getVolumeTypeForUpdatingDisks(CloudPlatform cloudPlatform) {
        if (cloudPlatform == CloudPlatform.AWS) {
            return AWS_DISK_TYPE;
        }
        return null;
    }

    private List<String> getVolumesOnCloudProvider(DistroXTestDto distroXTestDto, TestContext tc, CloudbreakClient client, boolean rootVolumes) {
        List<String> updatedInstances = distroxUtil.getInstanceIds(distroXTestDto, client, TEST_INSTANCE_GROUP);
        CloudFunctionality cloudFunctionality = getCloudFunctionality(tc);
        if (rootVolumes) {
            return cloudFunctionality.listInstancesRootVolumeIds(distroXTestDto.getName(), updatedInstances);
        } else {
            return cloudFunctionality.listInstancesVolumeIds(distroXTestDto.getName(), updatedInstances);
        }
    }

    private Set<VolumeV4Response> getVolumes(DistroXTestDto distroXTestDto) {
        StackV4Response stackV4Response = distroXTestDto.getResponse();
        InstanceGroupV4Response instanceGroup = stackV4Response.getInstanceGroups().stream().filter(ig -> ig.getName().equals(TEST_INSTANCE_GROUP))
                .findFirst().orElseThrow();
        Set<VolumeV4Response> attachedVolumesWithGroup = instanceGroup.getTemplate().getAttachedVolumes().stream()
                .filter(volumeV4Response -> volumeV4Response.getCount() > 0).collect(Collectors.toSet());
        return attachedVolumesWithGroup;
    }

    private List<ResourceV4Response> getRootVolumes(DistroXTestDto distroXTestDto, CloudPlatform cloudPlatform) {
        StackV4Response stackV4Response = distroXTestDto.getResponse();
        return stackV4Response.getResources().stream()
                .filter(res -> res.getResourceType().equals(PLATFORM_RESOURCE_TYPE_MAP.get(cloudPlatform))
                        && res.getInstanceGroup().equals(TEST_INSTANCE_GROUP))
                .toList();
    }

    protected CloudFunctionality getCloudFunctionality(TestContext testContext) {
        return testContext.getCloudProvider().getCloudFunctionality();
    }

    private void validateRootDisks(DistroXTestDto distroXTestDto, TestContext tc, CloudbreakClient client, CloudPlatform cloudPlatform) {
        String expectedVolumeType = getVolumeTypeForUpdatingDisks(cloudPlatform);

        List<String> rootVolumes = getVolumesOnCloudProvider(distroXTestDto, tc, client, true);
        if (CollectionUtils.isEmpty(rootVolumes)) {
            throw new TestFailException(String.format("Root volume is not present on instances on Cloud Provider for group %s",
                    TEST_INSTANCE_GROUP));
        }

        List<Volume> rootVolumesAttributes = getCloudFunctionality(tc).describeVolumes(rootVolumes);
        rootVolumesAttributes.forEach(vol -> {
            if (vol.getSize() != ROOT_UPDATE_SIZE || (expectedVolumeType != null && !expectedVolumeType.equalsIgnoreCase(vol.getType()))) {
                throw new TestFailException(String.format("Root Volume Modification did not complete successfully for instances on cloud provider in group %s",
                        TEST_INSTANCE_GROUP));
            }
        });

        List<ResourceV4Response> rootVolumesInGroup = getRootVolumes(distroXTestDto, cloudPlatform);
        if (CollectionUtils.isEmpty(rootVolumesInGroup)) {
            throw new TestFailException(String.format("Root volume is not present on instances in CB for group %s",
                    TEST_INSTANCE_GROUP));

        }
    }
}
