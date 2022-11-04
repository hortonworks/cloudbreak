package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ValidateRdsUpgradeOnCloudProviderResult extends AbstractValidateRdsUpgradeEvent {

    @JsonCreator
    public ValidateRdsUpgradeOnCloudProviderResult(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
