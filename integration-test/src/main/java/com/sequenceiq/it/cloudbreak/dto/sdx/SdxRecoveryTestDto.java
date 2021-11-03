package com.sequenceiq.it.cloudbreak.dto.sdx;

import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractSdxTestDto;
import com.sequenceiq.sdx.api.model.UpgradeRecoveryRequest;
import com.sequenceiq.sdx.api.model.SdxRecoveryResponse;
import com.sequenceiq.sdx.api.model.UpgradeRecoveryType;

@Prototype
public class SdxRecoveryTestDto extends AbstractSdxTestDto<UpgradeRecoveryRequest, SdxRecoveryResponse, SdxRecoveryTestDto> {

    public SdxRecoveryTestDto(UpgradeRecoveryRequest request, TestContext testContext) {
        super(request, testContext);
    }

    public SdxRecoveryTestDto(TestContext testContext) {
        super(new UpgradeRecoveryRequest(), testContext);
    }

    public SdxRecoveryTestDto() {
        super(SdxRecoveryTestDto.class.getSimpleName().toUpperCase());
    }

    @Override
    public SdxRecoveryTestDto valid() {
        return withRecoveryType(UpgradeRecoveryType.RECOVER_WITHOUT_DATA);
    }

    public SdxRecoveryTestDto withRecoveryType(UpgradeRecoveryType type) {
        getRequest().setType(type);
        return this;
    }

}
