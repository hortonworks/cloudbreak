package com.sequenceiq.it.cloudbreak.dto.sdx;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.diagnostics.model.DiagnosticsCollectionRequest;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.diagnostics.BaseDiagnosticsTestDto;

@Prototype
public class SdxDiagnosticsTestDto extends BaseDiagnosticsTestDto<DiagnosticsCollectionRequest, SdxDiagnosticsTestDto, SdxClient> {

    public SdxDiagnosticsTestDto(TestContext testContex) {
        super(new DiagnosticsCollectionRequest(), testContex, SdxClient.class);
    }

    @Override
    public SdxDiagnosticsTestDto valid() {
        withDefaults();
        Set<String> hostGroups = new HashSet<>();
        hostGroups.add("master");
        withHostGroups(hostGroups);
        return this;
    }

    @Override
    public SdxDiagnosticsTestDto awaitForFlow(RunningParameter runningParameter) {
        return getTestContext().awaitForFlow(this, runningParameter);
    }

    public SdxDiagnosticsTestDto withSdx() {
        return withSdx(null);
    }

    public SdxDiagnosticsTestDto withSdx(String sdx) {
        SdxTestDto sdxTestDto = sdx == null ? getTestContext().given(SdxTestDto.class)
                : getTestContext().given(sdx, SdxTestDto.class);
        getRequest().setStackCrn(sdxTestDto.getResponse().getCrn());
        return this;
    }
}
