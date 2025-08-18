package com.sequenceiq.environment.credential.v1.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.api.v1.environment.model.response.SetupCrossRealmTrustResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.CancelCrossRealmTrustResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.FinishSetupCrossRealmTrustResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.RepairCrossRealmTrustResponse;

@Component
public class EnvironmentHybridConverter {

    public SetupCrossRealmTrustResponse convertToPrepareCrossRealmTrustResponse(FlowIdentifier source) {
        SetupCrossRealmTrustResponse response = new SetupCrossRealmTrustResponse();
        response.setFlowIdentifier(source);
        return response;
    }

    public FinishSetupCrossRealmTrustResponse convertToFinishCrossRealmTrustResponse(FlowIdentifier source) {
        FinishSetupCrossRealmTrustResponse response = new FinishSetupCrossRealmTrustResponse();
        response.setFlowIdentifier(source);
        return response;
    }

    public CancelCrossRealmTrustResponse convertToCancelCrossRealmTrustResponse(FlowIdentifier source) {
        CancelCrossRealmTrustResponse response = new CancelCrossRealmTrustResponse();
        response.setFlowIdentifier(source);
        return response;
    }

    public RepairCrossRealmTrustResponse convertToRepairCrossRealmTrustResponse(FlowIdentifier source) {
        RepairCrossRealmTrustResponse response = new RepairCrossRealmTrustResponse();
        response.setFlowIdentifier(source);
        return response;
    }
}
