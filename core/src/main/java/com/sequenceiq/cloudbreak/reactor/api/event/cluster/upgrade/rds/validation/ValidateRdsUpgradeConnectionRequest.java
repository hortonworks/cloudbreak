package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseConnectionProperties;

public class ValidateRdsUpgradeConnectionRequest extends AbstractValidateRdsUpgradeEvent {

    private final DatabaseConnectionProperties canaryProperties;

    @JsonCreator
    public ValidateRdsUpgradeConnectionRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("canaryProperties") DatabaseConnectionProperties canaryProperties) {
        super(stackId);
        this.canaryProperties = canaryProperties;
    }

    public DatabaseConnectionProperties getCanaryProperties() {
        return canaryProperties == null ? new DatabaseConnectionProperties() : canaryProperties;
    }

    @Override
    public String toString() {
        return "ValidateRdsUpgradeConnectionRequest{" +
                "canaryProperties='" + canaryProperties + '\'' +
                "} " + super.toString();
    }
}