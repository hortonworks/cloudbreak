package com.sequenceiq.it.cloudbreak.dto.freeipa;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;

import java.util.Map;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.UpscaleRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.UpscaleResponse;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractFreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;

@Prototype
public class FreeIpaUpscaleTestDto extends AbstractFreeIpaTestDto<UpscaleRequest, UpscaleResponse, FreeIpaUpscaleTestDto>
        implements EnvironmentAware {
    protected FreeIpaUpscaleTestDto(TestContext testContext) {
        super(new UpscaleRequest(), testContext);
    }

    @Override
    public CloudbreakTestDto valid() {
        return withEnvironmentCrn(getTestContext().given(EnvironmentTestDto.class).getCrn());

    }

    private FreeIpaUpscaleTestDto withEnvironmentCrn(String environmentCrn) {
        getRequest().setEnvironmentCrn(environmentCrn);
        return this;
    }

    public FreeIpaUpscaleTestDto withAvailabilityType(AvailabilityType availabilityType) {
        getRequest().setTargetAvailabilityType(availabilityType);
        return this;
    }

    @Override
    public String getCrn() {
        return getRequest().getEnvironmentCrn();
    }

    public FreeIpaUpscaleTestDto await(Status state) {
        return getTestContext().await(this, Map.of("status", state), emptyRunningParameter());
    }

    @Override
    public String getEnvironmentCrn() {
        return getRequest().getEnvironmentCrn();
    }

}
