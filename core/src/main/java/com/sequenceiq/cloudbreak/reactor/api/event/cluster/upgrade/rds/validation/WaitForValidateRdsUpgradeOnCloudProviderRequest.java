package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseConnectionProperties;
import com.sequenceiq.flow.api.model.FlowIdentifier;

public class WaitForValidateRdsUpgradeOnCloudProviderRequest extends AbstractValidateRdsUpgradeEvent {

    private final FlowIdentifier flowIdentifier;

    private final DatabaseConnectionProperties canaryProperties;

    @JsonCreator
    public WaitForValidateRdsUpgradeOnCloudProviderRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("flowIdentifier") FlowIdentifier flowIdentifier,
            @JsonProperty("canaryProperties") DatabaseConnectionProperties canaryProperties) {
        super(stackId);
        this.flowIdentifier = flowIdentifier;
        this.canaryProperties = canaryProperties;
    }

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    public DatabaseConnectionProperties getCanaryProperties() {
        return canaryProperties;
    }

    @Override
    public String toString() {
        return "WaitForValidateRdsUpgradeOnCloudProviderRequest{" +
                "flowIdentifier=" + flowIdentifier +
                ", canaryProperties=" + canaryProperties +
                "} " + super.toString();
    }
}