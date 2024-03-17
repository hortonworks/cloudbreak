package com.sequenceiq.it.cloudbreak.testcase.e2e.imagevalidation;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.assertion.hybrid.HybridCloudAssertions;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ImageSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.hybrid.HybridCloudE2ETest;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.it.util.imagevalidation.ImageValidatorE2ETest;
import com.sequenceiq.it.util.imagevalidation.ImageValidatorE2ETestUtil;

public class YarnImageValidatorE2ETest extends HybridCloudE2ETest implements ImageValidatorE2ETest {

    @Inject
    private ImageValidatorE2ETestUtil imageValidatorE2ETestUtil;

    @Inject
    private HybridCloudAssertions hybridCloudAssertions;

    @Override
    protected void setupTest(TestContext testContext) {
        imageValidatorE2ETestUtil.setupTest(testContext, this);
        super.setupTest(testContext);
        setWorkloadPassword(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Cloudbreak",
            when = "a valid SDX create request with provided image settings is sent for child environment",
            then = "SDX should be available and ssh should be successful")
    public void testHybridSDXWithBaseImage(TestContext testContext) {
        testContext
                .given(CHILD_SDX_IMAGE_SETTINGS_KEY, ImageSettingsTestDto.class, CHILD_CLOUD_PLATFORM)
                    .withImageCatalog(imageValidatorE2ETestUtil.getImageCatalogName())
                    .withImageId(imageValidatorE2ETestUtil.getImageUuid())
                    .withOs(null);
        createChildDatalake(testContext);
        testContext
                .given(CHILD_SDX_KEY, SdxInternalTestDto.class, CHILD_CLOUD_PLATFORM)
                .then(hybridCloudAssertions.validateDatalakeSshAuthentication())
                .validate();
    }

    @Override
    public String getImageId(TestContext testContext) {
        SdxInternalTestDto sdxInternalTestDto = testContext.get(CHILD_SDX_KEY);
        return sdxInternalTestDto.getResponse().getStackV4Response().getImage().getId();
    }

    @Override
    public boolean isPrewarmedImageTest() {
        return false;
    }
}
