package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.util.CollectionUtils;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.resource.ResourceV4Response;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class SdxRootVolumeModificationTest extends PreconditionSdxE2ETest {

    private static final Map<String, String> SDX_TAGS = Map.of("sdxTagKey", "sdxTagValue");

    private static final String TEST_INSTANCE_GROUP = "master";

    private static final int ROOT_UPDATE_SIZE = 310;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private SdxUtil sdxUtil;

    @Test(dataProvider = TEST_CONTEXT, timeOut = 9000000)
    @Description(
            given = "there is an available environment with a running datalake",
            when = "root volume modification is called on the datalake",
            then = "root volume should be modified and the datalake should be up and running"
    )
    public void testSdxRootVolumeModification(TestContext testContext) {
        String instanceType = testContext.getCloudProvider().getDatahubCustomInstanceType();
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
                .when(sdxTestClient.describeInternal())
                .awaitForHealthyInstances()
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.updateDisks(ROOT_UPDATE_SIZE, testContext.getCloudProvider().verticalScaleVolumeType(), TEST_INSTANCE_GROUP,
                        DiskType.ROOT_DISK))
                .awaitForFlow()
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.describeInternalWithResources())
                .then((tc, testDto, client) -> {
                    validateRootDisks(testDto, tc, client);
                    return testDto;
                })
                .validate();
    }

    private List<String> getVolumesOnCloudProvider(SdxInternalTestDto sdxTestDto, TestContext tc, SdxClient client) {
        List<String> updatedInstances = sdxUtil.getInstanceIds(sdxTestDto, client, TEST_INSTANCE_GROUP);
        CloudFunctionality cloudFunctionality = getCloudFunctionality(tc);
        return cloudFunctionality.listInstancesRootVolumeIds(sdxTestDto.getName(), updatedInstances);
    }

    private List<ResourceV4Response> getRootVolumes(SdxInternalTestDto sdxTestDto, TestContext tc) {
        SdxClusterDetailResponse sdxClusterDetailResponse = sdxTestDto.getResponse();
        return sdxClusterDetailResponse.getStackV4Response().getResources().stream()
                .filter(res -> res.getResourceType().equals(tc.getCloudProvider().getRootDiskResourceType())
                        && res.getInstanceGroup().equals(TEST_INSTANCE_GROUP))
                .toList();
    }

    private void validateRootDisks(SdxInternalTestDto sdxTestDto, TestContext tc, SdxClient client) {
        String operation = "Update";
        int expectedDiskSize = ROOT_UPDATE_SIZE;
        String expectedVolumeType = tc.getCloudProvider().verticalScaleVolumeType();

        List<String> rootVolumes = getVolumesOnCloudProvider(sdxTestDto, tc, client);
        if (CollectionUtils.isEmpty(rootVolumes)) {
            throw new TestFailException(String.format("Root volume is not present on instances on Cloud Provider for group %s",
                    TEST_INSTANCE_GROUP));
        }

        List<Volume> rootVolumesAttributes = getCloudFunctionality(tc).describeVolumes(rootVolumes);
        rootVolumesAttributes.forEach(vol -> {
            if (vol.getSize() != expectedDiskSize || (expectedVolumeType != null && !expectedVolumeType.equalsIgnoreCase(vol.getType()))) {
                throw new TestFailException(String.format("%s Disk did not complete successfully for instances on cloud provider in group %s",
                        operation, TEST_INSTANCE_GROUP));
            }
        });

        List<ResourceV4Response> rootVolumesInGroup = getRootVolumes(sdxTestDto, tc);
        if (CollectionUtils.isEmpty(rootVolumesInGroup)) {
            throw new TestFailException(String.format("Root volume is not present on instances in CB for group %s",
                    TEST_INSTANCE_GROUP));

        }
    }
}
