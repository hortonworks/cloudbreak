package com.sequenceiq.it.cloudbreak.assertion.audit;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.dto.recipe.RecipeTestDto;

@Component
public class RecipeAuditGrpcServiceAssertion extends AuditGrpcServiceAssertion<RecipeTestDto, CloudbreakClient> {

    @Override
    protected String getCreateEventName() {
        return "CreateRecipe";
    }

    @Override
    protected String getDeleteEventName() {
        return "DeleteRecipe";
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
