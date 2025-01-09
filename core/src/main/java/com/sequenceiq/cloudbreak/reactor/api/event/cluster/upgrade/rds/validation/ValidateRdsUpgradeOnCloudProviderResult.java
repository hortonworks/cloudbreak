package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseConnectionProperties;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.flow.api.model.FlowIdentifier;

public class ValidateRdsUpgradeOnCloudProviderResult extends AbstractValidateRdsUpgradeEvent {

    private final TargetMajorVersion version;

    private final String reason;

    private final FlowIdentifier flowIdentifier;

    private final DatabaseConnectionProperties canaryProperties;

    @JsonCreator
    public ValidateRdsUpgradeOnCloudProviderResult(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("version") TargetMajorVersion version,
            @JsonProperty("reason") String reason,
            @JsonProperty("flowIdentifier") FlowIdentifier flowIdentifier,
            @JsonProperty("canaryProperties")  DatabaseConnectionProperties canaryProperties) {
        super(stackId);
        this.version = version;
        this.reason = reason;
        this.flowIdentifier = flowIdentifier;
        this.canaryProperties = canaryProperties;
    }

    public TargetMajorVersion getVersion() {
        return version;
    }

    public String getReason() {
        return reason;
    }

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    public DatabaseConnectionProperties getCanaryProperties() {
        return canaryProperties;
    }

    @Override
    public String toString() {
        return "ValidateRdsUpgradeOnCloudProviderResult{" +
                "version=" + version +
                ", reason='" + reason + '\'' +
                ", flowIdentifier=" + flowIdentifier +
                ", canaryProperties=" + canaryProperties +
                "} " + super.toString();
    }
}