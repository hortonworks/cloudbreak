package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeEvent.CLUSTER_UPGRADE_FAILED_EVENT;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeFailedEvent extends StackEvent {

    private Exception exception;

    private DetailedStackStatus detailedStatus;

    public ClusterUpgradeFailedEvent(Long stackId, Exception exception, DetailedStackStatus detailedStatus) {
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
