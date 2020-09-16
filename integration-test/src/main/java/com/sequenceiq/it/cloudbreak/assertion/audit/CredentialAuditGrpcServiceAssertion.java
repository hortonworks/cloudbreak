package com.sequenceiq.it.cloudbreak.assertion.audit;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;

@Component
public class CredentialAuditGrpcServiceAssertion extends AuditGrpcServiceAssertion<CredentialTestDto, EnvironmentClient> {

    @NotNull
    @Override
    protected String getCreateEventName() {
        return "CreateCredential";
    }

    @NotNull
    @Override
    protected String getDeleteEventName() {
        return "DeleteCredential";
    }

    @NotNull
    @Override
    protected String getModifyEventName() {
        return "ModifyCredential";
    }

    @Override
    protected Crn.Service getService() {
        return Crn.Service.ENVIRONMENTS;
    }

    @Override
    protected boolean shouldCheckFlowEvents() {
        return false;
    }
}
