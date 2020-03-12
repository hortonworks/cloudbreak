package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeEvent.CLUSTER_UPGRADE_FAILED_EVENT;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeFailedEvent extends StackEvent {

    private Exception exception;

    public ClusterUpgradeFailedEvent(Long stackId, Exception exception) {
        super(stackId);
        this.exception = exception;
    }

    public static ClusterUpgradeFailedEvent from(StackEvent event, Exception exception) {
        return new ClusterUpgradeFailedEvent(event.getResourceId(), exception);
    }

    @Override
    public String selector() {
        return CLUSTER_UPGRADE_FAILED_EVENT.event();
    }

    public Exception getException() {
        return exception;
    }
}
