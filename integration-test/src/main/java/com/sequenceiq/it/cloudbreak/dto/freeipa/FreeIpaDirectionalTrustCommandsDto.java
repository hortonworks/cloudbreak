package com.sequenceiq.it.cloudbreak.dto.freeipa;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands.DirectionalTrustSetupCommandsResponse;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractFreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;

@Prototype
public class FreeIpaDirectionalTrustCommandsDto
        extends AbstractFreeIpaTestDto<String, DirectionalTrustSetupCommandsResponse, FreeIpaDirectionalTrustCommandsDto> {

    public FreeIpaDirectionalTrustCommandsDto(TestContext testContext) {
        super(testContext.given(EnvironmentTestDto.class).getCrn(), testContext);
    }

    @Override
    public FreeIpaDirectionalTrustCommandsDto valid() {
        getFreeIpaName();
        return withEnvironmentCrn();
    }

    public FreeIpaDirectionalTrustCommandsDto withEnvironmentCrn(String environmentCrn) {
        setRequest(environmentCrn);
        return this;
    }

    public FreeIpaDirectionalTrustCommandsDto withEnvironmentCrn() {
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
