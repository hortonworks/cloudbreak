package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.util.CollectionUtils;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.volume.VolumeV4Response;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseAvailabilityType;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseRequest;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
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

public class DistroXVolumesVerticalScaleTest extends AbstractE2ETest {

    enum DiskOperationType {
        MODIFY, DELETE, ADD
    }

    private static final Map<String, String> SDX_TAGS = Map.of("sdxTagKey", "sdxTagValue");

    private static final Map<String, String> DX_TAGS = Map.of("distroxTagKey", "distroxTagValue");

    private static final String TEST_INSTANCE_GROUP = "compute";

    private static final int UPDATE_SIZE = 500;

    private static final int ADD_DISK_SIZE = 200;

    private static final long NUM_DISK_TO_ADD = 2;

    private static final String AWS_DISK_TYPE = "gp3";

    private static final String AZURE_DISK_TYPE = "StandardSSD_LRS";

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private DistroxUtil distroxUtil;

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
            when = "disk resize is called first then delete disks is called on the Datahubs",
            then = "attached EBS volumes on datahubs must be modified to the new type and size" +
                    " and then deleted, the new datahub should be up and running"
    )
    public void testDistroXVolumesVerticalScale(TestContext testContext) {
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
            .withoutDatabase()
            .withCloudStorage(getCloudStorageRequest(testContext))
            .when(sdxTestClient.createInternal())
            .await(SdxClusterStatusResponse.RUNNING)
            .awaitForHealthyInstances()
            .given("dx", DistroXTestDto.class)
            .withInstanceGroupsEntity(new DistroXInstanceGroupsBuilder(testContext)
                    .defaultHostGroup()
                    .withStorageOptimizedInstancetype()
                    .withInstanceType(instanceType)
                    .build())
            .addTags(DX_TAGS)
            .when(distroXTestClient.create(), RunningParameter.key("dx"))
            .await(STACK_AVAILABLE, RunningParameter.key("dx"))
            .awaitForHealthyInstances()
            .given("dx", DistroXTestDto.class)
            .when(distroXTestClient.updateDisks(UPDATE_SIZE, getVolumeType(DiskOperationType.MODIFY, cloudPlatform), TEST_INSTANCE_GROUP),
                    RunningParameter.key("dx"))
            .await(STACK_AVAILABLE, RunningParameter.key("dx"))
            .awaitForHealthyInstances()
            .given("dx", DistroXTestDto.class)
            .when(distroXTestClient.get(), RunningParameter.key("dx"))
            .then((tc, testDto, client) -> {
                validateDisks(testDto, tc, client, cloudPlatform, DiskOperationType.MODIFY);
                return testDto;
            })
            .awaitForHealthyInstances()
            .given("dx", DistroXTestDto.class)
            .when(distroXTestClient.deleteDisks(), RunningParameter.key("dx"))
            .await(STACK_AVAILABLE, RunningParameter.key("dx"))
            .awaitForHealthyInstances()
            .given("dx", DistroXTestDto.class)
            .when(distroXTestClient.get(), RunningParameter.key("dx"))
            .then((tc, testDto, client) -> {
                validateDeleteDisk(testDto, tc, client);
                return testDto;
            })
            .awaitForHealthyInstances()
            .given("dx", DistroXTestDto.class)
            .when(distroXTestClient.addDisks(ADD_DISK_SIZE, getVolumeType(DiskOperationType.ADD, cloudPlatform), TEST_INSTANCE_GROUP, NUM_DISK_TO_ADD),
                    RunningParameter.key("dx"))
            .await(STACK_AVAILABLE, RunningParameter.key("dx"))
            .awaitForHealthyInstances()
            .given("dx", DistroXTestDto.class)
            .when(distroXTestClient.get(), RunningParameter.key("dx"))
            .then((tc, testDto, client) -> {
                validateDisks(testDto, tc, client, cloudPlatform, DiskOperationType.ADD);
                return testDto;
            })
            .validate();
    }

    private void validateDeleteDisk(DistroXTestDto distroXTestDto, TestContext tc, CloudbreakClient client) {
        List<String> attachedVolumes = getVolumesOnCloudProvider(distroXTestDto, tc, client);
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

    private void validateDisks(DistroXTestDto distroXTestDto, TestContext tc, CloudbreakClient client, CloudPlatform cloudPlatform,
            DiskOperationType operationType) {
        String operation = operationType == DiskOperationType.ADD ? "Add" : "Update";
        int diskSize = operationType == DiskOperationType.ADD ? ADD_DISK_SIZE : UPDATE_SIZE;
        String expectedVolumeType = getVolumeType(operationType, cloudPlatform);

        List<String> attachedVolumes = getVolumesOnCloudProvider(distroXTestDto, tc, client);
        if (CollectionUtils.isEmpty(attachedVolumes) || (operationType == DiskOperationType.ADD && attachedVolumes.size() != NUM_DISK_TO_ADD)) {
            throw new TestFailException(String.format("%s Disk did not complete successfully on cloud provider for instances in group %s. " +
                    "Attached Volumes %s on cloud provider does not match with expected number of Volumes", operation, TEST_INSTANCE_GROUP, attachedVolumes));
        }

        Set<VolumeV4Response> attachedVolumesWithGroup = getVolumes(distroXTestDto);
        if (CollectionUtils.isEmpty(attachedVolumesWithGroup) || (operationType == DiskOperationType.ADD
                && attachedVolumesWithGroup.stream().mapToInt(VolumeV4Response::getCount).sum() != NUM_DISK_TO_ADD)) {
            throw new TestFailException(String.format("%s Disk did not complete successfully for instances in group %s. " +
                    "Attached Volumes %s does not match with expected number of Volumes in CB", operation, TEST_INSTANCE_GROUP, attachedVolumesWithGroup));

        }
        attachedVolumesWithGroup.forEach(vol -> {
            if (vol.getSize() != diskSize || (expectedVolumeType != null && !expectedVolumeType.equalsIgnoreCase(vol.getType()))) {
                throw new TestFailException(String.format("%s Disk did not complete successfully for instances in group %s in CB", operation,
                        TEST_INSTANCE_GROUP));
            }
        });

        List<Volume> attachedVolumesAttributes = getCloudFunctionality(tc).describeVolumes(attachedVolumes);
        attachedVolumesAttributes.forEach(vol -> {
            if (vol.getSize() != diskSize || (expectedVolumeType != null && !expectedVolumeType.equalsIgnoreCase(vol.getType()))) {
                throw new TestFailException(String.format("%s Disk did not complete successfully for instances on cloud provider in group %s",
                        operation, TEST_INSTANCE_GROUP));
            }
        });

    }

    private String getVolumeType(DiskOperationType operationType, CloudPlatform cloudPlatform) {
        if (cloudPlatform == CloudPlatform.AWS) {
            return AWS_DISK_TYPE;
        } else if (cloudPlatform == CloudPlatform.AZURE && operationType == DiskOperationType.ADD) {
            return AZURE_DISK_TYPE;
        }
        return null;
    }

    private List<String> getVolumesOnCloudProvider(DistroXTestDto distroXTestDto, TestContext tc, CloudbreakClient client) {
        List<String> updatedInstances = distroxUtil.getInstanceIds(distroXTestDto, client, TEST_INSTANCE_GROUP);
        CloudFunctionality cloudFunctionality = getCloudFunctionality(tc);
        return cloudFunctionality.listInstancesVolumeIds(distroXTestDto.getName(), updatedInstances);
    }

    private Set<VolumeV4Response> getVolumes(DistroXTestDto distroXTestDto) {
        StackV4Response stackV4Response = distroXTestDto.getResponse();
        InstanceGroupV4Response instanceGroup = stackV4Response.getInstanceGroups().stream().filter(ig -> ig.getName().equals(TEST_INSTANCE_GROUP))
                .findFirst().orElseThrow();
        Set<VolumeV4Response> attachedVolumesWithGroup = instanceGroup.getTemplate().getAttachedVolumes().stream()
                .filter(volumeV4Response -> volumeV4Response.getCount() > 0).collect(Collectors.toSet());
        return attachedVolumesWithGroup;
    }

    protected CloudFunctionality getCloudFunctionality(TestContext testContext) {
        return testContext.getCloudProvider().getCloudFunctionality();
    }
}
