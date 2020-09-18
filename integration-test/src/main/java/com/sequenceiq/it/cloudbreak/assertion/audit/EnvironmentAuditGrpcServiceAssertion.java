package com.sequenceiq.it.cloudbreak.assertion.audit;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;

@Component
public class EnvironmentAuditGrpcServiceAssertion extends AuditGrpcServiceAssertion<EnvironmentTestDto, EnvironmentClient> {

    @Override
    protected String getStopEventName() {
        return "StopEnvironment";
    }

    @Override
    protected String getDeleteEventName() {
        return "DeleteEnvironment";
    }

    @Override
    protected String getStartEventName() {
        return "StartEnvironment";
    }

    @Override
    protected String getCreateEventName() {
        return "CreateEnvironment";
    }

    @Override
    protected Crn.Service getService() {
        return Crn.Service.ENVIRONMENTS;
    }
}
