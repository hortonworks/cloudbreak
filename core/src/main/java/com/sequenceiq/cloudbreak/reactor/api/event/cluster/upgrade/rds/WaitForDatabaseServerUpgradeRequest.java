package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.flow.api.model.FlowIdentifier;

public class WaitForDatabaseServerUpgradeRequest extends AbstractUpgradeRdsEvent {
    private final FlowIdentifier flowIdentifier;

    @JsonCreator
    public WaitForDatabaseServerUpgradeRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("version") TargetMajorVersion version,
            @JsonProperty("flowIdentifier") FlowIdentifier flowIdentifier) {
        super(stackId, version);
        this.flowIdentifier = flowIdentifier;
    }

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    @Override
    public String toString() {
        return "WaitForDatabaseServerUpgradeRequest{" +
                "flowIdentifier=" + flowIdentifier +
                "} " + super.toString();
    }
}
