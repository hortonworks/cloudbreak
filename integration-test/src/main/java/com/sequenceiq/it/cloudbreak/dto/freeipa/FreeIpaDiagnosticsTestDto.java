package com.sequenceiq.it.cloudbreak.dto.freeipa;

import com.sequenceiq.freeipa.api.v1.diagnostics.model.DiagnosticsCollectionRequest;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.diagnostics.BaseDiagnosticsTestDto;

@Prototype
public class FreeIpaDiagnosticsTestDto extends BaseDiagnosticsTestDto<DiagnosticsCollectionRequest, FreeIpaDiagnosticsTestDto, FreeIpaClient> {

    public FreeIpaDiagnosticsTestDto(TestContext testContext) {
        super(new DiagnosticsCollectionRequest(), testContext);
    }

    @Override
    public FreeIpaDiagnosticsTestDto valid() {
        FreeIpaTestDto freeIpaTestDto = getTestContext().given(FreeIpaTestDto.class);
        withEnvironmentCrn(freeIpaTestDto.getResponse().getEnvironmentCrn());
        withDefaults();
        return this;
    }

    public FreeIpaDiagnosticsTestDto withEnvironmentCrn(String stackCrn) {
        getRequest().setEnvironmentCrn(stackCrn);
        return this;
    }

}
