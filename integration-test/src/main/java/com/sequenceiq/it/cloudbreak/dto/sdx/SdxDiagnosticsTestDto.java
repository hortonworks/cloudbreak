package com.sequenceiq.it.cloudbreak.dto.sdx;

import com.sequenceiq.cloudbreak.api.endpoint.v4.diagnostics.model.DiagnosticsCollectionRequest;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.diagnostics.BaseDiagnosticsTestDto;

@Prototype
public class SdxDiagnosticsTestDto extends BaseDiagnosticsTestDto<DiagnosticsCollectionRequest, SdxDiagnosticsTestDto, SdxClient> {

    public SdxDiagnosticsTestDto(TestContext testContex) {
        super(new DiagnosticsCollectionRequest(), testContex);
    }

    @Override
    public SdxDiagnosticsTestDto valid() {
        SdxTestDto sdxTestDto = getTestContext().given(SdxTestDto.class);
        withStackCrn(sdxTestDto.getResponse().getStackCrn());
        withDefaults();
        return this;
    }

    public SdxDiagnosticsTestDto withStackCrn(String stackCrn) {
        getRequest().setStackCrn(stackCrn);
        return this;
    }
}
