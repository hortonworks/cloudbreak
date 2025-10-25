package com.sequenceiq.it.cloudbreak.dto.freeipa;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands.TrustSetupCommandsResponse;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractFreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;

@Prototype
public class FreeIpaTrustCommandsDto extends AbstractFreeIpaTestDto<String, TrustSetupCommandsResponse, FreeIpaTrustCommandsDto> {

    public FreeIpaTrustCommandsDto(TestContext testContext) {
        super(testContext.given(EnvironmentTestDto.class).getCrn(), testContext);
    }

    @Override
    public FreeIpaTrustCommandsDto valid() {
        getFreeIpaName();
        return withEnvironmentCrn();
    }

    public FreeIpaTrustCommandsDto withEnvironmentCrn(String environmentCrn) {
        setRequest(environmentCrn);
        return this;
    }

    public FreeIpaTrustCommandsDto withEnvironmentCrn() {
        setRequest(getEnvironmentCrn());
        return this;
    }

    public String getEnvironmentCrn() {
        EnvironmentTestDto environmentTestDto = getTestContext().get(EnvironmentTestDto.class);
        if (environmentTestDto != null && environmentTestDto.getResponse() != null) {
            return environmentTestDto.getResponse().getCrn();
        } else {
            throw new IllegalArgumentException(String.format("Environment has not been provided for this FreeIPA Trust Commands: '%s' response!", getName()));
        }
    }

    public String getFreeIpaName() {
        FreeIpaTestDto freeIpaTestDto = getTestContext().get(FreeIpaTestDto.class);
        if (freeIpaTestDto != null && freeIpaTestDto.getResponse() != null) {
            return freeIpaTestDto.getResponse().getName();
        } else {
            throw new IllegalArgumentException(String.format("Freeipa has not been provided for this FreeIPA Trust Commands: '%s' response!", getName()));
        }
    }
}
