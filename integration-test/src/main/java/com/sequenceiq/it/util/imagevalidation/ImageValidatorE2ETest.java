package com.sequenceiq.it.util.imagevalidation;

import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;

public interface ImageValidatorE2ETest {

    default String getFreeIpaImageId(TestContext testContext) {
        return testContext.get(FreeIpaTestDto.class).getResponse().getImage().getId();
    }

    default String getCbImageId(TestContext testContext) {
        return testContext.get(SdxInternalTestDto.class).getResponse().getStackV4Response().getImage().getId();
    }

}
