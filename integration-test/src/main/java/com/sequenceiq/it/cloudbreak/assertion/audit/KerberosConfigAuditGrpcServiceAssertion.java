package com.sequenceiq.it.cloudbreak.assertion.audit;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.dto.kerberos.KerberosTestDto;

@Component
public class KerberosConfigAuditGrpcServiceAssertion extends AuditGrpcServiceAssertion<KerberosTestDto, FreeIpaClient> {

    @NotNull
    @Override
    protected String getDeleteEventName() {
        return "DeleteKerberosConfig";
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
