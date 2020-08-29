package com.sequenceiq.it.cloudbreak.dto.freeipa;

import com.sequenceiq.freeipa.api.v1.diagnostics.model.DiagnosticsCollectionRequest;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.diagnostics.BaseDiagnosticsTestDto;

@Prototype
public class FreeIpaDiagnosticsTestDto extends BaseDiagnosticsTestDto<DiagnosticsCollectionRequest, FreeIpaDiagnosticsTestDto, FreeIpaClient> {

    private String freeIpaCrn;

    public FreeIpaDiagnosticsTestDto(TestContext testContext) {
        super(new DiagnosticsCollectionRequest(), testContext, FreeIpaClient.class);
    }

    @Override
    public FreeIpaDiagnosticsTestDto valid() {
        withDefaults();
        return this;
    }

    @Override
    public FreeIpaDiagnosticsTestDto awaitForFlow(RunningParameter runningParameter) {
        return getTestContext().awaitForFlow(this, runningParameter);
    }

    public FreeIpaDiagnosticsTestDto withFreeIpa(String freeIpa) {
        FreeIpaTestDto freeIpaTestDto = getTestContext().given(freeIpa, FreeIpaTestDto.class);
        getRequest().setEnvironmentCrn(freeIpaTestDto.getResponse().getEnvironmentCrn());
        this.freeIpaCrn = freeIpaTestDto.getResponse().getCrn();
        return this;
    }

    public String getFreeIpaCrn() {
        return freeIpaCrn;
    }
}
