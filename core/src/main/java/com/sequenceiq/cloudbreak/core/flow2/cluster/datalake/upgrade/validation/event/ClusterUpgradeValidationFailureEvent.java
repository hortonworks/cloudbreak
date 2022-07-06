package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeValidationFailureEvent extends StackEvent {

    private final Exception exception;

    @JsonCreator
    public ClusterUpgradeValidationFailureEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("exception") Exception exception) {
        super(FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT.event(), resourceId);
        this.exception = exception;
    }

    @Override
    public String selector() {
        return FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT.event();
    }

    public Exception getException() {
        return exception;
    }
}
