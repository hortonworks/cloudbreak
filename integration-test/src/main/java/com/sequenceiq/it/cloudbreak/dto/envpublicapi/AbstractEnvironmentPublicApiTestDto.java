package com.sequenceiq.it.cloudbreak.dto.envpublicapi;

import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractTestDto;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentPublicApiClient;

public abstract class AbstractEnvironmentPublicApiTestDto<R, S, T extends CloudbreakTestDto> extends AbstractTestDto<R, S, T, EnvironmentPublicApiClient> {
    protected AbstractEnvironmentPublicApiTestDto(R request, TestContext testContext) {
        super(request, testContext);
    }

    public String getEnvironmentCrn() {
        return getTestContext().get(EnvironmentTestDto.class).getResponse().getCrn();
    }
}
