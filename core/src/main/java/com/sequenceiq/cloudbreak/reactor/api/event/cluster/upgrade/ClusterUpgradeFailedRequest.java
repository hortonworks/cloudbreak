package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeFailedRequest extends StackEvent {

    private final Exception exception;

    private final DetailedStackStatus detailedStatus;

    @JsonCreator
    public ClusterUpgradeFailedRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("detailedStatus") DetailedStackStatus detailedStackStatus) {
        super(stackId);
        this.exception = exception;
        this.detailedStatus = detailedStackStatus;
    }

    public Exception getException() {
        return exception;
    }

    public DetailedStackStatus getDetailedStatus() {
        return detailedStatus;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ClusterUpgradeFailedRequest.class.getSimpleName() + "[", "]")
                .add("exception=" + exception)
                .add("detailedStatus=" + detailedStatus)
                .add(super.toString())
                .toString();
    }
}
