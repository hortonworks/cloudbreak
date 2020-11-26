package com.sequenceiq.it.cloudbreak.assertion.audit;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.dto.blueprint.BlueprintTestDto;

@Component
public class BlueprintAuditGrpcServiceAssertion extends AuditGrpcServiceAssertion<BlueprintTestDto, CloudbreakClient> {

    @Override
    protected String getCreateEventName() {
        return "CreateBlueprint";
    }

    @Override
    protected String getDeleteEventName() {
        return "DeleteBlueprint";
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
