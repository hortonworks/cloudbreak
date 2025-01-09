package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseConnectionProperties;

public class WaitForValidateRdsUpgradeOnCloudProviderResult extends AbstractValidateRdsUpgradeEvent {

    private final String reason;

    private final DatabaseConnectionProperties canaryProperties;

    @JsonCreator
    public WaitForValidateRdsUpgradeOnCloudProviderResult(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("reason") String reason,
            @JsonProperty("canaryProperties") DatabaseConnectionProperties canaryProperties) {
        super(stackId);
        this.reason = reason;
        this.canaryProperties = canaryProperties;
    }

    public String getReason() {
        return reason;
    }

    public DatabaseConnectionProperties getCanaryProperties() {
        return canaryProperties;
    }

    @Override
    public String toString() {
        return "ValidateRdsUpgradeOnCloudProviderResult{" +
                ", reason='" + reason + '\'' +
                ", canaryProperties=" + canaryProperties +
                "} " + super.toString();
    }
}