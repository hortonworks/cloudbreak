package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationStateSelectors.FAILED_CLUSTER_UPGRADE_PREPARATION_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class ClusterUpgradePreparationFailureEvent extends StackFailureEvent {

    private final Exception exception;

    @JsonCreator
    public ClusterUpgradePreparationFailureEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("exception") Exception exception) {
        super(FAILED_CLUSTER_UPGRADE_PREPARATION_EVENT.event(), resourceId, exception);
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }

    @Override
    public String toString() {
        return "ClusterUpgradePreparationFailureEvent{" +
                "selector='" + selector() + '\'' +
                ", exception='" + exception + '\'' +
                '}' + super.toString();
    }
}
