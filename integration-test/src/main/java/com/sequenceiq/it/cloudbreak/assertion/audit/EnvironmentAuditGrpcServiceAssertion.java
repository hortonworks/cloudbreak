package com.sequenceiq.it.cloudbreak.assertion.audit;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;

@Component
public class EnvironmentAuditGrpcServiceAssertion extends AuditGrpcServiceAssertion<EnvironmentTestDto, EnvironmentClient> {

    @NotNull
    @Override
    protected String getStopEventName() {
        return "StopEnvironment";
    }

    @NotNull
    @Override
    protected String getDeleteEventName() {
        return "DeleteEnvironment";
    }

    @NotNull
    @Override
    protected String getStartEventName() {
        return "StartEnvironment";
    }

    @NotNull
    @Override
    protected String getCreateEventName() {
        return "CreateEnvironment";
    }

    @Override
    protected Crn.Service getService() {
        return Crn.Service.ENVIRONMENTS;
    }
}
