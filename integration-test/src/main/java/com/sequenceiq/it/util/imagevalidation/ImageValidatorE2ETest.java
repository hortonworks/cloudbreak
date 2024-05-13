package com.sequenceiq.it.util.imagevalidation;

import java.util.Optional;

import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;

public interface ImageValidatorE2ETest {

    default String getFreeIpaImageId(TestContext testContext) {
        return Optional.ofNullable(testContext.get(FreeIpaTestDto.class))
                .map(testDto -> testDto.getResponse().getImage().getId())
                .orElse(null);
    }

    default String getCbImageId(TestContext testContext) {
        return Optional.ofNullable(testContext.get(SdxInternalTestDto.class))
                .map(testDto -> testDto.getResponse().getStackV4Response().getImage().getId())
                .orElse(null);
    }

}
