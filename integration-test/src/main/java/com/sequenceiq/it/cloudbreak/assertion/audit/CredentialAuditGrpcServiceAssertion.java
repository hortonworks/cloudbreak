package com.sequenceiq.it.cloudbreak.assertion.audit;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;

@Component
public class CredentialAuditGrpcServiceAssertion extends AuditGrpcServiceAssertion<CredentialTestDto, EnvironmentClient> {

    @Override
    protected OperationInfo getCreateOperationInfo() {
        return OperationInfo.builder().withEventName("CreateCredential").build();
    }

    @Override
    protected OperationInfo getDeleteOperationInfo() {
        return OperationInfo.builder().withEventName("DeleteCredential").build();
    }

    @Override
    protected OperationInfo getModifyOperationInfo() {
        return OperationInfo.builder().withEventName("ModifyCredential").build();
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
