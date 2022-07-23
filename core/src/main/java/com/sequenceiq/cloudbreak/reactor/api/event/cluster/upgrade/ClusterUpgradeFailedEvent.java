package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.CLASS;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeEvent.CLUSTER_UPGRADE_FAILED_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeFailedEvent extends StackEvent {

    @JsonTypeInfo(use = CLASS, property = "@type")
    private final Exception exception;

    private final DetailedStackStatus detailedStatus;

    @JsonCreator
    public ClusterUpgradeFailedEvent(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("detailedStatus") DetailedStackStatus detailedStatus) {
        super(stackId);
        this.exception = exception;
        this.detailedStatus = detailedStatus;
    }

    public static ClusterUpgradeFailedEvent from(StackEvent event, Exception exception, DetailedStackStatus detailedStatus) {
        return new ClusterUpgradeFailedEvent(event.getResourceId(), exception, detailedStatus);
    }

    @Override
    public String selector() {
        return CLUSTER_UPGRADE_FAILED_EVENT.event();
    }

    public Exception getException() {
        return exception;
    }

    public DetailedStackStatus getDetailedStatus() {
        return detailedStatus;
    }
}
