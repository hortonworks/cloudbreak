package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.recovery.bringup;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class DatalakeRecoverySetupNewInstancesFailedEvent extends StackEvent {

    private final Exception exception;

    private final DetailedStackStatus detailedStatus;

    public DatalakeRecoverySetupNewInstancesFailedEvent(Long stackId, Exception exception, DetailedStackStatus detailedStatus) {
        super(stackId);
        this.exception = exception;
        this.detailedStatus = detailedStatus;
    }

    public static DatalakeRecoverySetupNewInstancesFailedEvent from(StackEvent event, Exception exception, DetailedStackStatus detailedStatus) {
        return new DatalakeRecoverySetupNewInstancesFailedEvent(event.getResourceId(), exception, detailedStatus);
    }

    public Exception getException() {
        return exception;
    }

    public DetailedStackStatus getDetailedStatus() {
        return detailedStatus;
    }

    @Override
    public String toString() {
        return "DatalakeRecoverySetupNewInstancesFailedEvent{" +
                "exception=" + exception +
                ", detailedStatus=" + detailedStatus +
                "} " + super.toString();
    }
}
