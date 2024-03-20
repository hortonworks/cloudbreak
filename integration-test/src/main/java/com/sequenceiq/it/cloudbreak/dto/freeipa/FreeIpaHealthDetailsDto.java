package com.sequenceiq.it.cloudbreak.dto.freeipa;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.HealthDetailsFreeIpaResponse;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractFreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;

@Prototype
public class FreeIpaHealthDetailsDto extends AbstractFreeIpaTestDto<String, HealthDetailsFreeIpaResponse, FreeIpaHealthDetailsDto> {

    public FreeIpaHealthDetailsDto(TestContext testContext) {
        super(testContext.given(EnvironmentTestDto.class).getCrn(), testContext);
    }

    @Override
    public FreeIpaHealthDetailsDto valid() {
        return withEnvironmentCrn();
    }

    public FreeIpaHealthDetailsDto withEnvironmentCrn(String environmentCrn) {
        setRequest(environmentCrn);
        return this;
    }

    public FreeIpaHealthDetailsDto withEnvironmentCrn() {
        setRequest(getEnvironmentCrn());
        return this;
    }

    public String getEnvironmentCrn() {
        EnvironmentTestDto environmentTestDto = getTestContext().get(EnvironmentTestDto.class);
        if (environmentTestDto != null && environmentTestDto.getResponse() != null) {
            return environmentTestDto.getResponse().getCrn();
        } else {
            throw new IllegalArgumentException(String.format("Environment has not been provided for this FreeIPA Health Details: '%s' response!",
                    getName()));
        }
    }

    public String getFreeIpaCrn() {
        FreeIpaTestDto freeIpaTestDto = getTestContext().get(FreeIpaTestDto.class);
        if (freeIpaTestDto != null && freeIpaTestDto.getResponse() != null) {
            return freeIpaTestDto.getResponse().getCrn();
        } else {
            throw new IllegalArgumentException(String.format("FreeIPA has not been provided for this FreeIPA Health Details: '%s' response!",
                    getName()));
        }
    }
}
