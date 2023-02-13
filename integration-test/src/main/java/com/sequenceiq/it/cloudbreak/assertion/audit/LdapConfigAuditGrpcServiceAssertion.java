package com.sequenceiq.it.cloudbreak.assertion.audit;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.it.cloudbreak.dto.ldap.LdapTestDto;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

@Component
public class LdapConfigAuditGrpcServiceAssertion extends AuditGrpcServiceAssertion<LdapTestDto, FreeIpaClient> {

    @Override
    protected OperationInfo getCreateOperationInfo() {
        return OperationInfo.builder().withEventName("CreateLdapConfig").build();
    }

    @Override
    protected OperationInfo getDeleteOperationInfo() {
        return OperationInfo.builder().withEventName("DeleteLdapConfig").build();
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
