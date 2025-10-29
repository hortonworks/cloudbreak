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
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseAvailabilityType;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseRequest;
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
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.DistroxUtil;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class DistroXVolumesModificationTest extends AbstractE2ETest {

    private static final Map<CloudPlatform, ResourceType> PLATFORM_RESOURCE_TYPE_MAP = ImmutableMap.of(CloudPlatform.AWS, ResourceType.AWS_ROOT_DISK,
            CloudPlatform.AZURE, ResourceType.AZURE_DISK);

    private static final Map<String, String> SDX_TAGS = Map.of("sdxTagKey", "sdxTagValue");

    private static final Map<String, String> DX_TAGS = Map.of("distroxTagKey", "distroxTagValue");

    private static final String TEST_INSTANCE_GROUP = "coordinator";

    private static final int UPDATE_SIZE = 500;

    private static final int ROOT_UPDATE_SIZE = 310;

    private static final String AWS_DISK_TYPE = "gp2";

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
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultCredential(testContext);
        createEnvironmentWithFreeIpa(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT, timeOut = 9000000)
    @Description(
            given = "there is an available environment with a running datahub",
            when = "root disk modification is done first, then additional disk resize is called first",
            then = "attached EBS volumes on datahubs must be modified to the new type and size"
    )
    public void testDistroXVolumesModification(TestContext testContext) {
        CloudPlatform cloudPlatform = testContext.getCloudPlatform();
        String instanceType = CloudPlatform.AWS.equals(cloudPlatform) ? "m5d.2xlarge" : "Standard_D8s_v3";

        DistroXDatabaseRequest distroXDatabaseRequest = new DistroXDatabaseRequest();
        distroXDatabaseRequest.setAvailabilityType(DistroXDatabaseAvailabilityType.NON_HA);

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
            .then((tc, testDto, client) -> selinuxAssertions.validateAll(tc, testDto, false, true))
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
            .then((tc, testDto, client) -> selinuxAssertions.validateAll(tc, testDto, false, true))
            .given("dx", DistroXTestDto.class)
            .when(distroXTestClient.updateDisks(ROOT_UPDATE_SIZE, getVolumeType(cloudPlatform), TEST_INSTANCE_GROUP,
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
            .when(distroXTestClient.updateDisks(UPDATE_SIZE, getVolumeType(cloudPlatform),
                TEST_INSTANCE_GROUP, DiskType.ADDITIONAL_DISK), RunningParameter.key("dx"))
            .await(STACK_AVAILABLE, RunningParameter.key("dx"))
            .awaitForHealthyInstances()
            .given("dx", DistroXTestDto.class)
            .when(distroXTestClient.get(), RunningParameter.key("dx"))
            .then((tc, testDto, client) -> {
                validateDisks(testDto, tc, client, cloudPlatform);
                return testDto;
            })
            .validate();
    }

    private void validateDisks(DistroXTestDto distroXTestDto, TestContext tc, CloudbreakClient client, CloudPlatform cloudPlatform) {
        String expectedVolumeType = getVolumeType(cloudPlatform);

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

    private String getVolumeType(CloudPlatform cloudPlatform) {
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
        String expectedVolumeType = getVolumeType(cloudPlatform);

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
