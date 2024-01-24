package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static java.lang.String.format;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.InstanceUtil;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class SdxVolumesVerticalScaleTest extends PreconditionSdxE2ETest {

    private static final Map<String, String> SDX_TAGS = Map.of("sdxTagKey", "sdxTagValue");

    private static final String TEST_INSTANCE_GROUP = "master";

    private static final int UPDATE_SIZE = 300;

    @Inject
    private SdxTestClient sdxTestClient;

    @Test(dataProvider = TEST_CONTEXT, timeOut = 9000000)
    @Description(
            given = "there is an available environment with a running datalake",
            when = "disk resize is called first then delete disks is called on the datalake",
            then = "attached EBS volumes on datalake must be modified to the new type and size" +
                    " and then deleted, the new datalake should be up and running"
    )
    public void testSdxVolumesVerticalScale(TestContext testContext) {
        CloudPlatform cloudPlatform = testContext.getCloudPlatform();
        String volumeType = CloudPlatform.AWS.equals(cloudPlatform) ? "gp3" : null;
        String instanceType = CloudPlatform.AWS.equals(cloudPlatform) ? "m5.2xlarge" : "Standard_D8s_v3";
        testContext
            .given("telemetry", TelemetryTestDto.class)
            .withLogging()
            .withReportClusterLogs()
            .given(SdxInternalTestDto.class)
            .withTelemetry("telemetry")
            .withInstanceType(instanceType)
            .addTags(SDX_TAGS)
            .withoutDatabase()
            .withCloudStorage(getCloudStorageRequest(testContext))
            .when(sdxTestClient.createInternal())
            .await(SdxClusterStatusResponse.RUNNING)
            .awaitForHealthyInstances()
            .given(SdxInternalTestDto.class)
            .when(sdxTestClient.updateDisks(UPDATE_SIZE, volumeType, "master"))
            .awaitForFlow()
            .await(SdxClusterStatusResponse.RUNNING)
            .awaitForHealthyInstances()
            .given(SdxInternalTestDto.class)
            .when(sdxTestClient.describeInternal())
            .then((tc, testDto, client) -> {
                validateVerticalScale(testDto, tc, cloudPlatform, volumeType);
                return testDto;
            })
            .validate();
    }

    private void validateVerticalScale(SdxInternalTestDto sdxTestDto, TestContext tc, CloudPlatform cloudPlatform, String volumeType) {

        SdxClusterDetailResponse sdxClusterDetailResponse = sdxTestDto.getResponse();
        InstanceGroupV4Response instanceGroup = sdxClusterDetailResponse.getStackV4Response().getInstanceGroups().stream()
                .filter(ig -> ig.getName().equals(TEST_INSTANCE_GROUP))
                .findFirst().orElseThrow();
        List<String> updatedInstances = InstanceUtil.getInstanceIds(List.of(instanceGroup), TEST_INSTANCE_GROUP);
        CloudFunctionality cloudFunctionality = getCloudFunctionality(tc);
        List<String> attachedVolumes = cloudFunctionality.listInstancesVolumeIds(sdxTestDto.getName(), updatedInstances);
        List<Volume> attachedVolumesAttributes = cloudFunctionality.describeVolumes(attachedVolumes);
        instanceGroup.getTemplate().getAttachedVolumes().forEach(vol -> {
            if (vol.getSize() != UPDATE_SIZE || (CloudPlatform.AWS.equals(cloudPlatform) && !volumeType.equals(vol.getType().toLowerCase(Locale.ROOT)))) {
                throw new TestFailException(getExceptionMessage(cloudPlatform, vol.getSize(), vol.getType().toLowerCase(Locale.ROOT), volumeType, false));
            }
        });
        attachedVolumesAttributes.forEach(vol -> {
            if (vol.getSize() != UPDATE_SIZE || (CloudPlatform.AWS.equals(cloudPlatform) && !volumeType.equals(vol.getType().toLowerCase(Locale.ROOT)))) {
                throw new TestFailException(getExceptionMessage(cloudPlatform, vol.getSize(), vol.getType().toLowerCase(Locale.ROOT), volumeType, true));
            }
        });
    }

    protected CloudFunctionality getCloudFunctionality(TestContext testContext) {
        return testContext.getCloudProvider().getCloudFunctionality();
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
}
