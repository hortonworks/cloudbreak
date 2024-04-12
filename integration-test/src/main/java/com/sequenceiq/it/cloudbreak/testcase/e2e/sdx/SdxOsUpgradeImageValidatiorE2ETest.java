package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;

import jakarta.inject.Inject;

import org.testng.SkipException;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.it.util.imagevalidation.ImageValidatorE2ETestUtil;
import com.sequenceiq.it.util.imagevalidation.PrewarmedImageValidatorE2ETest;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class SdxOsUpgradeImageValidatiorE2ETest extends PreconditionSdxE2ETest implements PrewarmedImageValidatorE2ETest {

    @Inject
    private ImageValidatorE2ETestUtil imageValidatorE2ETestUtil;

    private ImageV4Response latestImageWithSameRuntime;

    @Override
    protected void setupTest(TestContext testContext) {
        imageValidatorE2ETestUtil.setupTest(testContext, this);
        latestImageWithSameRuntime = imageValidatorE2ETestUtil.getLatestImageWithSameRuntimeAsImageUnderValidation(testContext)
                .orElseThrow(() -> new SkipException("There are no older images with the same runtime components, so OS upgrade testing is not possible"));
        super.setupTest(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "an SDX cluster with the latest image with same runtime components as the validated image is in available state",
            when = "upgrade called on the SDX cluster to the validated image",
            then = "SDX OS upgrade should be successful, the cluster should be up and running"
    )
    public void testSDXOsUpgrade(TestContext testContext) {
        testContext
                .given(SdxInternalTestDto.class)
                    .withCloudStorage()
                    .withoutDatabase()
                    .withTemplate(commonClusterManagerProperties().getInternalSdxBlueprintName())
                    .withImageCatalogNameAndImageId(commonCloudProperties().getImageValidation().getSourceCatalogName(), latestImageWithSameRuntime.getUuid())
                .when(sdxTestClient().createInternal())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .when(sdxTestClient().osUpgradeInternal(commonCloudProperties().getImageValidation().getImageUuid()))
                .await(SdxClusterStatusResponse.DATALAKE_UPGRADE_IN_PROGRESS, emptyRunningParameter().withWaitForFlow(Boolean.FALSE))
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .validate();
    }
}
