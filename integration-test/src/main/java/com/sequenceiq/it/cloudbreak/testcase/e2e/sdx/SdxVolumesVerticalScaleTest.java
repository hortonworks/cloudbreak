package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static java.lang.String.format;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.resource.ResourceV4Response;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.InstanceUtil;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;
import com.sequenceiq.it.cloudbreak.util.ssh.client.SshJClient;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class SdxVolumesVerticalScaleTest extends PreconditionSdxE2ETest {

    private static final Map<String, String> SDX_TAGS = Map.of("sdxTagKey", "sdxTagValue");

    private static final String TEST_INSTANCE_GROUP = "master";

    private static final int UPDATE_SIZE = 300;

    private static final Long ADD_DISKS_SIZE = 200L;

    private static final Long DISKS_COUNT = 2L;

    private static final String ADD_DISKS = "ADD_DISKS";

    private static final String MODIFY_DISKS = "MODIFY_DISKS";

    private static final String WHITESPACE_REGEX = "\\s+";

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private SdxUtil sdxUtil;

    @Inject
    private SshJClient sshJClient;

    @Test(dataProvider = TEST_CONTEXT, timeOut = 9000000)
    @Description(
            given = "there is an available environment with a running datalake",
            when = "disk resize is called first then delete disks is called on the datalake",
            then = "attached EBS volumes on datalake must be modified to the new type and size" +
                    " and then deleted, the new datalake should be up and running"
    )
    public void testSdxVolumesVerticalScale(TestContext testContext) {
        CloudPlatform cloudPlatform = testContext.getCloudPlatform();
        String instanceType = CloudPlatform.AWS.equals(cloudPlatform) ? "m5.4xlarge" : "Standard_D8s_v3";
        testContext
            .given("telemetry", TelemetryTestDto.class)
            .withLogging()
            .withReportClusterLogs()
            .given(SdxInternalTestDto.class)
            .withTelemetry("telemetry")
            .withInstanceType(instanceType)
            .addTags(SDX_TAGS)
            .withCloudStorage(getCloudStorageRequest(testContext))
            .when(sdxTestClient.createInternal())
            .await(SdxClusterStatusResponse.RUNNING)
            .when(sdxTestClient.describeInternalWithResources())
            .awaitForHealthyInstances()
            .given(SdxInternalTestDto.class)
            .when(sdxTestClient.updateDisks(UPDATE_SIZE, getVolumeType(MODIFY_DISKS, cloudPlatform, testContext), TEST_INSTANCE_GROUP,
                    DiskType.ADDITIONAL_DISK))
            .awaitForFlow()
            .await(SdxClusterStatusResponse.RUNNING)
            .awaitForHealthyInstances()
            .given(SdxInternalTestDto.class)
            .when(sdxTestClient.describeInternalWithResources())
            .then((tc, testDto, client) -> {
                validateVerticalScale(testDto, tc, cloudPlatform, getVolumeType(MODIFY_DISKS, cloudPlatform, testContext), MODIFY_DISKS);
                return testDto;
            })
            .given(SdxInternalTestDto.class)
            .when(sdxTestClient.addDisks(ADD_DISKS_SIZE, DISKS_COUNT, getVolumeType(ADD_DISKS, cloudPlatform, testContext), TEST_INSTANCE_GROUP,
                    CloudVolumeUsageType.GENERAL))
            .awaitForFlow()
            .await(SdxClusterStatusResponse.RUNNING)
            .awaitForHealthyInstances()
            .given(SdxInternalTestDto.class)
            .when(sdxTestClient.describeInternalWithResources())
            .then((tc, testDto, client) -> {
                validateVerticalScale(testDto, tc, cloudPlatform, getVolumeType(ADD_DISKS, cloudPlatform, testContext), ADD_DISKS);
                return testDto;
            })
            .validate();
    }

    private void validateVerticalScale(SdxInternalTestDto sdxTestDto, TestContext tc, CloudPlatform cloudPlatform, String volumeType, String operationType) {

        SdxClusterDetailResponse sdxClusterDetailResponse = sdxTestDto.getResponse();
        InstanceGroupV4Response instanceGroup = sdxClusterDetailResponse.getStackV4Response().getInstanceGroups().stream()
                .filter(ig -> ig.getName().equals(TEST_INSTANCE_GROUP))
                .findFirst().orElseThrow();
        List<String> updatedInstances = InstanceUtil.getInstanceIds(List.of(instanceGroup), TEST_INSTANCE_GROUP);
        Set<String> instanceIps = InstanceUtil.getInstancePrivateIps(List.of(instanceGroup), TEST_INSTANCE_GROUP);
        Map<String, String> instanceIpIdsMap = InstanceUtil.getInstanceIpIdMap(List.of(instanceGroup), TEST_INSTANCE_GROUP);
        validateFstab(sdxClusterDetailResponse.getStackV4Response(), instanceIps, instanceIpIdsMap);
        CloudFunctionality cloudFunctionality = tc.getCloudProvider().getCloudFunctionality();
        List<String> attachedVolumes = cloudFunctionality.listInstancesVolumeIds(sdxTestDto.getName(), updatedInstances);
        List<Volume> attachedVolumesAttributes = cloudFunctionality.describeVolumes(attachedVolumes);
        AtomicInteger cbVolumesCount = new AtomicInteger();
        instanceGroup.getTemplate().getAttachedVolumes().forEach(vol -> {
            if (operationType.equals(MODIFY_DISKS) && (vol.getSize() != UPDATE_SIZE
                    || (CloudPlatform.AWS.equals(cloudPlatform) && !volumeType.equalsIgnoreCase(vol.getType())))) {
                throw new TestFailException(getExceptionMessage(cloudPlatform, vol.getSize(), vol.getType(), volumeType, false));
            } else if (operationType.equals(ADD_DISKS) && vol.getSize() == ADD_DISKS_SIZE.intValue() && vol.getType().equalsIgnoreCase(volumeType)) {
                cbVolumesCount.getAndUpdate(val -> vol.getCount());
            }
        });
        AtomicInteger cloudProviderVolumesCount = new AtomicInteger();
        attachedVolumesAttributes.forEach(vol -> {
            if (operationType.equals(MODIFY_DISKS) && (vol.getSize() != UPDATE_SIZE
                    || (CloudPlatform.AWS.equals(cloudPlatform) && !volumeType.equalsIgnoreCase(vol.getType())))) {
                throw new TestFailException(getExceptionMessage(cloudPlatform, vol.getSize(), vol.getType(), volumeType, true));
            } else if (operationType.equals(ADD_DISKS) && vol.getSize() == ADD_DISKS_SIZE.intValue() && vol.getType().equalsIgnoreCase(volumeType)) {
                cloudProviderVolumesCount.getAndIncrement();
            }
        });
        if (operationType.equals(ADD_DISKS)) {
            validateOrThrow(cbVolumesCount.get(), "DATABASE");
            validateOrThrow(cbVolumesCount.get(), "CLOUD_PROVIDER");
        }
    }

    private String getExceptionMessage(CloudPlatform cloudPlatform, int size, String volumeType, String expectedVolumeType, boolean cloudProviderException) {
        StringBuilder sb = new StringBuilder(format("Disk Update did not complete successfully for instances in group %s", TEST_INSTANCE_GROUP));
        if (cloudProviderException) {
            sb.append(" on cloud provider");
        }
        sb.append(format(", expected size: %d :: actual size: %d", UPDATE_SIZE, size));
        if (CloudPlatform.AWS.equals(cloudPlatform)) {
            sb.append(format(", expected disk type: %s :: actual disk type: %s", expectedVolumeType, volumeType));
        }
        return sb.toString();
    }

    private void validateOrThrow(int volumesAdded, String exceptionType) {
        if (volumesAdded != DISKS_COUNT.intValue()) {
            String exceptionMessage = "Add Volumes Flow failed: " + (("DATABASE").equals(exceptionType) ? "Failed to update database"
                    : "Failed to add disks on cloud provider");
            throw new TestFailException(exceptionMessage);
        }
    }

    private String getVolumeType(String operationType, CloudPlatform cloudPlatform, TestContext testContext) {
        if ((cloudPlatform == CloudPlatform.AWS) || (cloudPlatform == CloudPlatform.AZURE && ADD_DISKS.equals(operationType))) {
            return testContext.getCloudProvider().verticalScaleVolumeType();
        }
        return null;
    }

    private void validateFstab(StackV4Response stackV4Response, Set<String> instanceIps, Map<String, String> instanceIpIdsMap) {
        Map<String, String> attributesByInstanceId = stackV4Response.getResources().stream()
                .filter(res -> instanceIpIdsMap.containsKey(res.getInstanceId()) && res.getResourceType().toString().contains("_VOLUMESET"))
                .collect(Collectors.toMap(res -> instanceIpIdsMap.get(res.getInstanceId()),
                        ResourceV4Response::getAttributes));
        Map<String, Pair<Integer, String>> fstabInfo = sshJClient.executeCommands(instanceIps, "sudo cat /etc/fstab");
        StringBuilder exceptionMessage = new StringBuilder();
        for (String instanceIp : fstabInfo.keySet()) {
            validateNoDuplicateMounts(fstabInfo.get(instanceIp).getRight());
            if (attributesByInstanceId.containsKey(instanceIp)) {
                try {
                    String savedFstab = normalizeFstab(new Json(attributesByInstanceId.get(instanceIp)).get(VolumeSetAttributes.class).getFstab());
                    String fstabFromSsh = normalizeFstab(fstabInfo.get(instanceIp).getRight());
                    if (!savedFstab.equalsIgnoreCase(fstabFromSsh)) {
                        exceptionMessage.append("Add Volumes Flow failed: Saved fstab information doesn't match what is present " +
                                        "in the cloud provider for instance Id ")
                                .append(instanceIp).append("!");
                    }
                } catch (IOException e) {
                    exceptionMessage.append("Unable to parse fstab stored in the database.");
                }
            } else {
                exceptionMessage.append("The fstab information for instance ID : ")
                        .append(instanceIp).append(" is not stored in the database.");
            }
        }
        if (!exceptionMessage.isEmpty()) {
            throw new TestFailException(exceptionMessage.toString());
        }
    }

    private void validateNoDuplicateMounts(String fstab) {
        Map<String, List<String>> uuidToPaths = fstab.lines()
                .filter(l -> l.startsWith("UUID="))
                .map(l -> l.split(WHITESPACE_REGEX))
                .collect(Collectors.groupingBy(
            parts -> parts[0], Collectors.mapping(parts -> parts[1], Collectors.toList())
                ));
        uuidToPaths.forEach((uuid, paths) -> {
            if (paths.size() > 1) {
                throw new TestFailException("Add Volumes test failed: Duplicate mounts found!");
            }
        });
    }

    private String normalizeFstab(String fstab) {
        return fstab.lines()
            .map(String::trim)
            .filter(l -> !l.isEmpty())
            .reduce("", (acc, line) -> acc + line.replaceAll(WHITESPACE_REGEX, " "));
    }
}
