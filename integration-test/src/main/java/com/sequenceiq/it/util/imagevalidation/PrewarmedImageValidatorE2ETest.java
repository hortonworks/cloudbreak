package com.sequenceiq.it.util.imagevalidation;

import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;

public interface PrewarmedImageValidatorE2ETest extends ImageValidatorE2ETest {

    @Override
    default String getImageId(TestContext testContext) {
        return testContext.get(SdxInternalTestDto.class).getResponse().getStackV4Response().getImage().getId();
    }

    @Override
    default boolean isPrewarmedImageTest() {
        return true;
    }
}
