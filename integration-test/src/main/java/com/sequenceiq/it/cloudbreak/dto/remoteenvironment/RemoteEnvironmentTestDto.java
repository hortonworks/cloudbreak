package com.sequenceiq.it.cloudbreak.dto.remoteenvironment;

import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractRemoteEnvironmentTestDto;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.SimpleRemoteEnvironmentResponse;

@Prototype
public class RemoteEnvironmentTestDto extends AbstractRemoteEnvironmentTestDto<Object, SimpleRemoteEnvironmentResponse, RemoteEnvironmentTestDto> {

    private static final int ORDER = 100;

    public RemoteEnvironmentTestDto(TestContext testContext) {
        super(null, testContext);
    }

    @Override
    public RemoteEnvironmentTestDto valid() {
        return this;
    }

    @Override
    public int order() {
        return ORDER;
    }
}
