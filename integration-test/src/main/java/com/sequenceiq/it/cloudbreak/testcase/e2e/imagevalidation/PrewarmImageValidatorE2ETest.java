package com.sequenceiq.it.cloudbreak.testcase.e2e.imagevalidation;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.image.DistroXImageTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
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

    @Override
    protected void setupTest(TestContext testContext) {
        imageValidatorE2ETestUtil.setupTest(testContext, this);
        createDefaultCredential(testContext);
        initializeDefaultBlueprints(testContext);
        createEnvironmentWithFreeIpa(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "a SDX internal request and a DistroX request",
            when = "a SDX internal create request is sent",
            then = "the SDX cluster and the corresponding DistroX cluster is created")
    public void testCreateInternalSdxAndDistrox(TestContext testContext) {
        testContext.given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs();
        testContext.given(SdxInternalTestDto.class)
                .withoutDatabase()
                .withCloudStorage(getCloudStorageRequest(testContext))
                .withTemplate(commonClusterManagerProperties().getInternalSdxBlueprintName())
                .withImageCatalogNameAndImageId(commonCloudProperties().getImageValidation().getSourceCatalogName(),
                        commonCloudProperties().getImageValidation().getImageUuid())
                .withTelemetry("telemetry")
                .when(sdxTestClient.createInternal())
                .await(SdxClusterStatusResponse.RUNNING)
                .when(sdxTestClient.describeInternal())
                .validate();
        testContext.given(DistroXTestDto.class)
                .withTemplate(commonClusterManagerProperties().getInternalDistroXBlueprintName())
                .withImageSettings(testContext
                        .given(DistroXImageTestDto.class)
                        .withImageCatalog(testContext.get(SdxInternalTestDto.class).getResponse().getStackV4Response().getImage().getCatalogName())
                        .withImageId(testContext.get(SdxInternalTestDto.class).getResponse().getStackV4Response().getImage().getId()))
                .when(distroXTestClient.create())
                .await(STACK_AVAILABLE)
                .then((context, distrox, client) -> {
                    distrox.getResponse();
                    return distrox;
                })
                .when(distroXTestClient.get())
                .validate();
    }

    @Override
    public String getImageId(TestContext testContext) {
        return testContext.get(SdxInternalTestDto.class).getResponse().getStackV4Response().getImage().getId();
    }

    @Override
    public boolean isPrewarmedImageTest() {
        return true;
    }
}
