package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.embeddeddb;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class UpgradeEmbeddedDBPreparationFailedEvent extends StackFailureEvent {

    private final DetailedStackStatus detailedStatus;

    @JsonCreator
    public UpgradeEmbeddedDBPreparationFailedEvent(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("detailedStatus") DetailedStackStatus detailedStatus) {
        super(stackId, exception);
        this.detailedStatus = detailedStatus;
    }

    public UpgradeEmbeddedDBPreparationFailedEvent(String selector, Long stackId, Exception exception, DetailedStackStatus detailedStatus) {
        super(selector, stackId, exception);
        this.detailedStatus = detailedStatus;
    }

    public DetailedStackStatus getDetailedStatus() {
        return detailedStatus;
    }
}
