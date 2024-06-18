package com.sequenceiq.it.util.imagevalidation;

import java.util.Optional;

import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;

public interface ImageValidatorE2ETest {

    default String getFreeIpaImageId(TestContext testContext) {
        return Optional.ofNullable(testContext.get(FreeIpaTestDto.class))
                .flatMap(testDto -> Optional.ofNullable(testDto.getResponse()))
                .map(response -> response.getImage().getId())
                .orElse(null);
    }

    default String getCbImageId(TestContext testContext) {
        return Optional.ofNullable(testContext.get(SdxInternalTestDto.class))
                .flatMap(testDto -> Optional.ofNullable(testDto.getResponse()))
                .map(response -> response.getStackV4Response().getImage().getId())
                .orElse(null);
    }

}
