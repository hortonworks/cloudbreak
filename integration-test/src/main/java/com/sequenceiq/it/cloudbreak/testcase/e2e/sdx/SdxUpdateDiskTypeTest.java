package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.volume.VolumeV4Response;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class SdxUpdateDiskTypeTest extends AbstractE2ETest {

    private static final Map<String, String> SDX_TAGS = Map.of("sdxTagKey", "sdxTagValue");

    @Inject
    private SdxTestClient sdxTestClient;

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
            given = "there is an available environment with a running SDX cluster",
            when = "resize and change disk type is called on the SDX cluster",
            then = "SDX resize should be successful, the new cluster should be up and running"
    )
    public void testSDXUpdateAndResize(TestContext testContext) {
        testContext
            .given("telemetry", TelemetryTestDto.class)
            .withLogging()
            .withReportClusterLogs()

            // Create original SDX cluster.
            .given(SdxInternalTestDto.class)
            .withTelemetry("telemetry")
            .addTags(SDX_TAGS)
            .withCloudStorage(getCloudStorageRequest(testContext))
            .when(sdxTestClient.createInternal())
            .await(SdxClusterStatusResponse.RUNNING)
            .awaitForHealthyInstances()
            .given(SdxInternalTestDto.class)
            .withTelemetry("telemetry")
            .when(sdxTestClient.detailedDescribeInternal())
            .given(SdxInternalTestDto.class)
            .withTelemetry("telemetry")
            .when(sdxTestClient.updateDisks())
            .await(SdxClusterStatusResponse.RUNNING)
            .given(SdxInternalTestDto.class)
            .withTelemetry("telemetry")
            .when(sdxTestClient.detailedDescribeInternal())
            .then(this::verifyMountedDisks)
            .validate();
    }

    private SdxInternalTestDto verifyMountedDisks(TestContext testContext, SdxInternalTestDto testDto, SdxClient sdxClient) {
        List<InstanceGroupV4Response> instanceGroups = testDto.getResponse().getStackV4Response().getInstanceGroups();
        Optional<InstanceGroupV4Response> instanceGroup = instanceGroups.stream()
                    .filter(ig -> ig.getName().equals("master")).findFirst();
        boolean result = false;
        if (instanceGroup.isPresent()) {
            Set<VolumeV4Response> attachedVolumes = instanceGroup.get().getTemplate().getAttachedVolumes();
            Optional<VolumeV4Response> volume = attachedVolumes.stream().findFirst();
            result = volume.map(vol -> vol.getType().equals("gp3") && vol.getSize() > 200).orElse(false);
        }
        Assertions.assertThat(result).withFailMessage("Failed to update attached disks").isTrue();
        return testDto;
    }
}
