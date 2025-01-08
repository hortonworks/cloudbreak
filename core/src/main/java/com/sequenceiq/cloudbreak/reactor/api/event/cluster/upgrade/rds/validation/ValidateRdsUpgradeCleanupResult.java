package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.flow.api.model.FlowIdentifier;

public class ValidateRdsUpgradeCleanupResult extends AbstractValidateRdsUpgradeEvent {

    private final FlowIdentifier flowIdentifier;

    @JsonCreator
    public ValidateRdsUpgradeCleanupResult(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("flowIdentifier") FlowIdentifier flowIdentifier) {
        super(stackId);
        this.flowIdentifier = flowIdentifier;
    }

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    @Override
    public String toString() {
        return "ValidateRdsUpgradeCleanupResult{" +
                "flowIdentifier=" + flowIdentifier +
                "} " + super.toString();
    }
}