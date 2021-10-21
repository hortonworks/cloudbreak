package com.sequenceiq.it.cloudbreak.dto.sdx;

import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractSdxTestDto;
import com.sequenceiq.sdx.api.model.SdxRecoveryRequest;
import com.sequenceiq.sdx.api.model.SdxRecoveryResponse;
import com.sequenceiq.sdx.api.model.SdxRecoveryType;

@Prototype
public class SdxRecoveryTestDto extends AbstractSdxTestDto<SdxRecoveryRequest, SdxRecoveryResponse, SdxRecoveryTestDto> {

    public SdxRecoveryTestDto(SdxRecoveryRequest request, TestContext testContext) {
        super(request, testContext);
    }

    public SdxRecoveryTestDto(TestContext testContext) {
        super(new SdxRecoveryRequest(), testContext);
    }

    public SdxRecoveryTestDto() {
        super(SdxRecoveryTestDto.class.getSimpleName().toUpperCase());
    }

    @Override
    public SdxRecoveryTestDto valid() {
        return withRecoveryType(SdxRecoveryType.RECOVER_WITHOUT_DATA);
    }

    public SdxRecoveryTestDto withRecoveryType(SdxRecoveryType type) {
        getRequest().setType(type);
        return this;
    }

}
