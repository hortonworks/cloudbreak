package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.flow.api.model.FlowIdentifier;

public class WaitForValidateRdsUpgradeCleanupRequest extends AbstractValidateRdsUpgradeEvent {

    private final String validateConnectionErrorMessage;

    private final FlowIdentifier flowIdentifier;

    @JsonCreator
    public WaitForValidateRdsUpgradeCleanupRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("validateConnectionErrorMessage") String validateConnectionErrorMessage,
            @JsonProperty("flowIdentifier") FlowIdentifier flowIdentifier) {
        super(stackId);
        this.validateConnectionErrorMessage = validateConnectionErrorMessage;
        this.flowIdentifier = flowIdentifier;
    }

    public String getValidateConnectionErrorMessage() {
        return validateConnectionErrorMessage;
    }

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    @Override
    public String toString() {
        return "WaitForValidateRdsUpgradeCleanupRequest{" +
                "validateConnectionErrorMessage='" + validateConnectionErrorMessage + '\'' +
                ", flowIdentifier=" + flowIdentifier +
                "} " + super.toString();
    }
}