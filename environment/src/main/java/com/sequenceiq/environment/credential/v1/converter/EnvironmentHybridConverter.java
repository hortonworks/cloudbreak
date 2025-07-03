package com.sequenceiq.environment.credential.v1.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentSetupCrossRealmTrustResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.FinishCrossRealmTrustResponse;

@Component
public class EnvironmentHybridConverter {

    public EnvironmentSetupCrossRealmTrustResponse convertToPrepareCrossRealmTrustResponse(FlowIdentifier source) {
        EnvironmentSetupCrossRealmTrustResponse response = new EnvironmentSetupCrossRealmTrustResponse();
        response.setFlowIdentifier(source);
        return response;
    }

    public FinishCrossRealmTrustResponse convertToFinishCrossRealmTrustResponse(FlowIdentifier source) {
        FinishCrossRealmTrustResponse response = new FinishCrossRealmTrustResponse();
        response.setFlowIdentifier(source);
        return response;
    }
}
