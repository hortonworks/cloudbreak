package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.FINISH_CLUSTER_UPGRADE_VALIDATION_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeValidationFinishedEvent extends StackEvent {

    private final Exception exception;

    public ClusterUpgradeValidationFinishedEvent(Long resourceId) {
        super(FINISH_CLUSTER_UPGRADE_VALIDATION_EVENT.name(), resourceId);
        exception = null;
    }

    @JsonCreator
    public ClusterUpgradeValidationFinishedEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("exception") Exception exception) {
        super(FINISH_CLUSTER_UPGRADE_VALIDATION_EVENT.name(), resourceId);
        this.exception = exception;
    }

    @Override
    public String selector() {
        return FINISH_CLUSTER_UPGRADE_VALIDATION_EVENT.name();
    }

    public Exception getException() {
        return exception;
    }
}
