package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ValidateRdsUpgradeCleanupRequest extends AbstractValidateRdsUpgradeEvent {

    @JsonCreator
    public ValidateRdsUpgradeCleanupRequest(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}