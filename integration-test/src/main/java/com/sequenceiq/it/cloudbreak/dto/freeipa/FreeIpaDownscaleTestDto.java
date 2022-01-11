package com.sequenceiq.it.cloudbreak.dto.freeipa;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;

import java.util.Map;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.FormFactor;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.DownscaleRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.DownscaleResponse;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractFreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;

@Prototype
public class FreeIpaDownscaleTestDto extends AbstractFreeIpaTestDto<DownscaleRequest, DownscaleResponse, FreeIpaDownscaleTestDto>
        implements EnvironmentAware {
    protected FreeIpaDownscaleTestDto(TestContext testContext) {
        super(new DownscaleRequest(), testContext);
    }

    @Override
    public CloudbreakTestDto valid() {
        return withEnvironmentCrn(getTestContext().given(EnvironmentTestDto.class).getCrn());

    }

    private FreeIpaDownscaleTestDto withEnvironmentCrn(String environmentCrn) {
        getRequest().setEnvironmentCrn(environmentCrn);
        return this;
    }

    public FreeIpaDownscaleTestDto withFormFactor(FormFactor formFactor) {
        getRequest().setTargetFormFactor(formFactor);
        return this;
    }

    @Override
    public String getCrn() {
        return getRequest().getEnvironmentCrn();
    }

    public FreeIpaDownscaleTestDto await(Status state) {
        return getTestContext().await(this, Map.of("status", state), emptyRunningParameter());
    }

    @Override
    public String getEnvironmentCrn() {
        return getRequest().getEnvironmentCrn();
    }

}