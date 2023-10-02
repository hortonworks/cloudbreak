package com.sequenceiq.it.cloudbreak.dto.freeipa;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.HealthDetailsFreeIpaResponse;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractFreeIpaTestDto;

@Prototype
public class FreeIpaHealthDetailsDto extends AbstractFreeIpaTestDto<String, HealthDetailsFreeIpaResponse, FreeIpaHealthDetailsDto> implements EnvironmentAware {

    private String environmentCrn;

    protected FreeIpaHealthDetailsDto(TestContext testContext) {
        super(null, testContext);
    }

    @Override
    public FreeIpaHealthDetailsDto valid() {
        return withEnvironmentCrn(getTestContext().given(FreeIpaHealthDetailsDto.class).getCrn());
    }

    private FreeIpaHealthDetailsDto withEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
        return this;
    }

    @Override
    public String getCrn() {
        return getEnvironmentCrn();
    }

    @Override
    public String getEnvironmentCrn() {
        return environmentCrn;
    }

}
