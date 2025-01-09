package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ValidateRdsUpgradeConnectionResult extends AbstractValidateRdsUpgradeEvent {

    private final String reason;

    @JsonCreator
    public ValidateRdsUpgradeConnectionResult(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("reason") String reason) {
        super(stackId);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "ValidateRdsUpgradeConnectionResult{" +
                "reason='" + reason + '\'' +
                "} " + super.toString();
    }
}