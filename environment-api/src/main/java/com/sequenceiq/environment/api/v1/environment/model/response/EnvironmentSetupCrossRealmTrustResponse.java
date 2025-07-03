package com.sequenceiq.environment.api.v1.environment.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.doc.ModelDescriptions;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "EnvironmentSetupV1CrossRealmTrustResponse")
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnvironmentSetupCrossRealmTrustResponse {

    @Schema(description = ModelDescriptions.FLOW_IDENTIFIER)
    private FlowIdentifier flowIdentifier;

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    public void setFlowIdentifier(FlowIdentifier flowIdentifier) {
        this.flowIdentifier = flowIdentifier;
    }

    @Override
    public String toString() {
        return "PrepareCrossRealmTrustResponse{" +
                "flowIdentifier=" + flowIdentifier +
                "} " + super.toString();
    }
}
