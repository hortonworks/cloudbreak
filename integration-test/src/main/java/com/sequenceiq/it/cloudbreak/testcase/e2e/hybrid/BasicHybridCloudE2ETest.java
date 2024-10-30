package com.sequenceiq.it.cloudbreak.testcase.e2e.hybrid;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.assertion.hybrid.HybridCloudAssertions;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ImageSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.it.util.imagevalidation.ImageValidatorE2ETest;
import com.sequenceiq.it.util.imagevalidation.ImageValidatorE2ETestUtil;

public class BasicHybridCloudE2ETest extends HybridCloudE2ETest implements ImageValidatorE2ETest  {

    @Inject
    private ImageValidatorE2ETestUtil imageValidatorE2ETestUtil;

    @Inject
    private HybridCloudAssertions hybridCloudAssertions;

    @Override
    protected void setupTest(TestContext testContext) {
        imageValidatorE2ETestUtil.setupTest(testContext);
        super.setupTest(testContext);
        setWorkloadPassword(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running AWS env and a child YARN env ",
            when = "creating an SDX in YARN ",
            then = "SDX is created and instances are accessible via ssh by valid username and password ",
            and = "instances are not accessible via ssh by invalid username and password "
    )
    public void testHybridSdx(TestContext testContext) {
        if (imageValidatorE2ETestUtil.isImageValidation()) {
            testContext
                    .given(CHILD_SDX_IMAGE_SETTINGS_KEY, ImageSettingsTestDto.class, CHILD_CLOUD_PLATFORM)
                        .withImageCatalog(imageValidatorE2ETestUtil.getImageCatalogName())
                        .withImageId(imageValidatorE2ETestUtil.getImageUuid())
                        .withOs(null);
        }
        createChildDatalake(testContext);
        testContext
                .given(CHILD_SDX_KEY, SdxInternalTestDto.class, CHILD_CLOUD_PLATFORM)
                .then(hybridCloudAssertions.validateDatalakeSshAuthentication())
                .validate();
    }

    @Override
    public String getCbImageId(TestContext testContext) {
        SdxInternalTestDto sdxInternalTestDto = testContext.get(CHILD_SDX_KEY);
        return sdxInternalTestDto.getResponse().getStackV4Response().getImage().getId();
    }
}
