package com.sequenceiq.it.cloudbreak.dto.remoteenvironment;

import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractRemoteEnvironmentTestDto;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.SimpleRemoteEnvironmentResponse;

@Prototype
public class ListRemoteEnvironmentsTestDto extends AbstractRemoteEnvironmentTestDto<Object, SimpleRemoteEnvironmentResponse, ListRemoteEnvironmentsTestDto> {

    private static final int ORDER = 100;

    public ListRemoteEnvironmentsTestDto(TestContext testContext) {
        super(null, testContext);
    }

    @Override
    public ListRemoteEnvironmentsTestDto valid() {
        return this;
    }

    @Override
    public int order() {
        return ORDER;
    }
}
