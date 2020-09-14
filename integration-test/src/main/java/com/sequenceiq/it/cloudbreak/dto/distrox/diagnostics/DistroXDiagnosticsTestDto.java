package com.sequenceiq.it.cloudbreak.dto.distrox.diagnostics;

import com.sequenceiq.distrox.api.v1.distrox.model.diagnostics.model.DiagnosticsCollectionV1Request;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.diagnostics.BaseDiagnosticsTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;

public class DistroXDiagnosticsTestDto extends BaseDiagnosticsTestDto<DiagnosticsCollectionV1Request, DistroXDiagnosticsTestDto, CloudbreakClient> {

    public DistroXDiagnosticsTestDto(TestContext testContext) {
        super(new DiagnosticsCollectionV1Request(), testContext, CloudbreakClient.class);
    }

    @Override
    public DistroXDiagnosticsTestDto valid() {
        withDefaults();
        return this;
    }

    @Override
    public DistroXDiagnosticsTestDto awaitForFlow(RunningParameter runningParameter) {
        return getTestContext().awaitForFlow(this, runningParameter);
    }

    public DistroXDiagnosticsTestDto withDistroX() {
        return withDistroX(null);
    }

    public DistroXDiagnosticsTestDto withDistroX(String distroX) {
        DistroXTestDto distroXTestDto = distroX == null ? getTestContext().given(DistroXTestDto.class)
                : getTestContext().given(distroX, DistroXTestDto.class);
        getRequest().setStackCrn(distroXTestDto.getResponse().getCrn());
        return this;
    }
}
