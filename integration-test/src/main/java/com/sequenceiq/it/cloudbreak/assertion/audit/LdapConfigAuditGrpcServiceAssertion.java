package com.sequenceiq.it.cloudbreak.assertion.audit;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.dto.ldap.LdapTestDto;

@Component
public class LdapConfigAuditGrpcServiceAssertion extends AuditGrpcServiceAssertion<LdapTestDto, FreeIpaClient> {

    @NotNull
    @Override
    protected String getCreateEventName() {
        return "CreateLdapConfig";
    }

    @NotNull
    @Override
    protected String getDeleteEventName() {
        return "DeleteLdapConfig";
    }

    @Override
    protected Crn.Service getService() {
        return Crn.Service.FREEIPA;
    }

    @Override
    protected boolean shouldCheckFlowEvents() {
        return false;
    }
}
