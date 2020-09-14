package com.sequenceiq.it.cloudbreak.dto.sdx;

import com.sequenceiq.cloudbreak.api.endpoint.v4.diagnostics.model.CmDiagnosticsCollectionRequest;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.diagnostics.BaseCMDiagnosticsTestDto;

@Prototype
public class SdxCMDiagnosticsTestDto extends BaseCMDiagnosticsTestDto<CmDiagnosticsCollectionRequest, SdxCMDiagnosticsTestDto, SdxClient> {

    public SdxCMDiagnosticsTestDto(TestContext testContext) {
        super(new CmDiagnosticsCollectionRequest(), testContext, SdxClient.class);
    }

    @Override
    public SdxCMDiagnosticsTestDto valid() {
        withDefaults();
        return this;
    }

    @Override
    public SdxCMDiagnosticsTestDto awaitForFlow(RunningParameter runningParameter) {
        return getTestContext().awaitForFlow(this, runningParameter);
    }

    public SdxCMDiagnosticsTestDto withSdx() {
        return withSdx(null);
    }

    public SdxCMDiagnosticsTestDto withSdx(String sdx) {
        SdxTestDto sdxTestDto = sdx == null ? getTestContext().given(SdxTestDto.class)
                : getTestContext().given(sdx, SdxTestDto.class);
        getRequest().setStackCrn(sdxTestDto.getResponse().getCrn());
        return this;
    }
}
