package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class UpgradeRdsFailedEvent extends StackFailureEvent {

    private final DetailedStackStatus detailedStatus;

    @JsonCreator
    public UpgradeRdsFailedEvent(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("detailedStatus") DetailedStackStatus detailedStatus) {
        super(stackId, exception);
        this.detailedStatus = detailedStatus;
    }

    public UpgradeRdsFailedEvent(String selector, Long stackId, Exception exception, DetailedStackStatus detailedStatus) {
        super(selector, stackId, exception);
        this.detailedStatus = detailedStatus;
    }

    public DetailedStackStatus getDetailedStatus() {
        return detailedStatus;
    }
}
