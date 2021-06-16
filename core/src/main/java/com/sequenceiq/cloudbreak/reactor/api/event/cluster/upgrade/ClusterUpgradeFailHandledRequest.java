package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeEvent.CLUSTER_UPGRADE_FAIL_HANDLED_EVENT;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeFailHandledRequest  extends StackEvent {

    private final Exception exception;

    private final DetailedStackStatus detailedStatus;

    public ClusterUpgradeFailHandledRequest(Long stackId, Exception e, DetailedStackStatus detailedStackStatus) {
        super(CLUSTER_UPGRADE_FAIL_HANDLED_EVENT.event(), stackId);
        this.exception = e;
        this.detailedStatus = detailedStackStatus;
    }

    public Exception getException() {
        return exception;
    }

    public DetailedStackStatus getDetailedStatus() {
        return detailedStatus;
    }
}
