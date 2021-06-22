package com.sequenceiq.it.cloudbreak.assertion.audit;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.dto.blueprint.BlueprintTestDto;

@Component
public class BlueprintAuditGrpcServiceAssertion extends AuditGrpcServiceAssertion<BlueprintTestDto, CloudbreakClient> {

    @Override
    protected OperationInfo getCreateOperationInfo() {
        return OperationInfo.builder().withEventName("CreateBlueprint").build();
    }

    @Override
    protected OperationInfo getDeleteOperationInfo() {
        return OperationInfo.builder().withEventName("DeleteBlueprint").build();
    }

    @Override
    protected Crn.Service getService() {
        return Crn.Service.DATAHUB;
    }

    @Override
    protected boolean shouldCheckFlowEvents() {
        return false;
    }
}
