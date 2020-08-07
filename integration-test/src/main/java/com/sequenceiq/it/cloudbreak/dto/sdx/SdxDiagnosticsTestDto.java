package com.sequenceiq.it.cloudbreak.dto.sdx;

import java.util.List;

import com.sequenceiq.cloudbreak.api.endpoint.v4.diagnostics.model.DiagnosticsCollectionRequest;
import com.sequenceiq.common.api.telemetry.model.DiagnosticsDestination;
import com.sequenceiq.common.api.telemetry.model.VmLog;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractTestDto;

@Prototype
public class SdxDiagnosticsTestDto extends AbstractTestDto<DiagnosticsCollectionRequest, FlowIdentifier, SdxDiagnosticsTestDto, SdxClient> {

    public SdxDiagnosticsTestDto(TestContext testContex) {
        super(new DiagnosticsCollectionRequest(), testContex);
    }

    @Override
    public SdxDiagnosticsTestDto valid() {
        SdxTestDto sdxTestDto = getTestContext().given(SdxTestDto.class);
        withStackCrn(sdxTestDto.getResponse().getStackCrn());
        withDestination(DiagnosticsDestination.CLOUD_STORAGE);
        return this;
    }

    public SdxDiagnosticsTestDto withStackCrn(String stackCrn) {
        getRequest().setStackCrn(stackCrn);
        return this;
    }

    public SdxDiagnosticsTestDto withDestination(DiagnosticsDestination destination) {
        getRequest().setDestination(destination);
        return this;
    }

    public SdxDiagnosticsTestDto withAdditionalLogs(List<VmLog> additionalLogs) {
        getRequest().setAdditionalLogs(additionalLogs);
        return this;
    }
}
