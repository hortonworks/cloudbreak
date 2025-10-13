package com.sequenceiq.it.cloudbreak.testcase.e2e.imagevalidation;

import java.util.Optional;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.StackImageV4Response;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.it.cloudbreak.assertion.image.ImageAssertions;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.it.util.imagevalidation.ImageValidatorE2ETest;
import com.sequenceiq.it.util.imagevalidation.ImageValidatorE2ETestUtil;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class PrewarmImageValidatorE2ETest extends AbstractE2ETest implements ImageValidatorE2ETest {

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private ImageValidatorE2ETestUtil imageValidatorE2ETestUtil;

    @Inject
    private ImageAssertions imageAssertions;

    private ImageV4Response imageUnderValidation;

    private Architecture architecture;

    @Override
    protected void setupTest(TestContext testContext) {
        imageValidatorE2ETestUtil.setupTest(testContext);
        createDefaultCredential(testContext);
        initializeDefaultBlueprints(testContext);
        imageUnderValidation = imageValidatorE2ETestUtil.getImageUnderValidation(testContext).orElseThrow();
        architecture = Architecture.fromStringWithFallback(imageUnderValidation.getArchitecture());
        createEnvironmentWithFreeIpa(testContext, architecture);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "a SDX internal request and a DistroX request",
            when = "a SDX internal create request is sent",
            then = "the SDX cluster and the corresponding DistroX cluster is created")
    public void testCreateInternalSdxAndDistrox(TestContext testContext) {
        String blueprintName;
        if (imageValidatorE2ETestUtil.isFreeIpaImageValidation()) {
            blueprintName = commonClusterManagerProperties().getDataEngDistroXBlueprintNameForCurrentRuntime();
        } else {
            blueprintName = commonClusterManagerProperties().getDataEngDistroXBlueprintName(imageUnderValidation.getVersion());
        }

        testContext.given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs();
        setupSdxImage(testContext, architecture);
        testContext
                .given(SdxInternalTestDto.class)
                    .withCloudStorage(getCloudStorageRequest(testContext))
                    .withTemplate(commonClusterManagerProperties().getInternalSdxBlueprintName())
                    .withTelemetry("telemetry")
                .when(sdxTestClient.createInternal())
                .then(imageAssertions.validateSdxInternalImageSetupTime())
                .await(SdxClusterStatusResponse.RUNNING)
                .when(sdxTestClient.describeInternal())
                .validate();
        testContext
                .given(DistroXTestDto.class)
                    .withArchitecture(architecture)
                    .withTemplate(blueprintName)
                .when(distroXTestClient.create())
                .then(imageAssertions.validateDistroXImageSetupTime())
                .await(STACK_AVAILABLE)
                .when(distroXTestClient.get())
                .validate();
        testContext.given(SdxInternalTestDto.class)
                .when(sdxTestClient.describeInternal())
                .then(((context, sdx, client) -> {
                    SdxClusterStatusResponse clusterStatus = sdx.getResponse().getStatus();
                    if (!clusterStatus.isRunning()) {
                        throw new TestFailException("SDX status is not running at the end of the validation. Current status is: " + clusterStatus);
                    }
                    return sdx;
                }))
                .validate();
    }

    private void setupSdxImage(TestContext testContext, Architecture architecture) {
        if (architecture == Architecture.ARM64) {
            // arm64 is not supported for SDX, so a default image selected by CB should be used
            testContext.given(SdxInternalTestDto.class)
                    .withDefaultImage();
        }
    }

    @Override
    public String getCbImageId(TestContext testContext) {
        ImageV4Response imageUnderValidation = imageValidatorE2ETestUtil.getImageUnderValidation(testContext).orElseThrow();
        return Architecture.fromStringWithFallback(imageUnderValidation.getArchitecture()) == Architecture.ARM64
                ? getDistroXImage(testContext)
                : ImageValidatorE2ETest.super.getCbImageId(testContext);
    }

    private String getDistroXImage(TestContext testContext) {
        return Optional.ofNullable(testContext.get(DistroXTestDto.class))
                .map(AbstractTestDto::getResponse)
                .map(StackV4Response::getImage)
                .map(StackImageV4Response::getId)
                .orElse(null);
    }
}
