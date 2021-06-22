package com.sequenceiq.it.cloudbreak.assertion.audit;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.dto.recipe.RecipeTestDto;

@Component
public class RecipeAuditGrpcServiceAssertion extends AuditGrpcServiceAssertion<RecipeTestDto, CloudbreakClient> {

    @Override
    protected OperationInfo getCreateOperationInfo() {
        return OperationInfo.builder().withEventName("CreateRecipe").build();
    }

    @Override
    protected OperationInfo getDeleteOperationInfo() {
        return OperationInfo.builder().withEventName("DeleteRecipe").build();
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
