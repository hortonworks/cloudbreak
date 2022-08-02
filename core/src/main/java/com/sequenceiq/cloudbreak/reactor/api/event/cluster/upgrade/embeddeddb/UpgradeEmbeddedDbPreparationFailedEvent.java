package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.embeddeddb;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class UpgradeEmbeddedDbPreparationFailedEvent extends StackFailureEvent {

    private final DetailedStackStatus detailedStatus;

    public UpgradeEmbeddedDbPreparationFailedEvent(Long stackId, Exception exception, DetailedStackStatus detailedStatus) {
        super(stackId, exception);
        this.detailedStatus = detailedStatus;
    }

    public UpgradeEmbeddedDbPreparationFailedEvent(String selector, Long stackId, Exception exception, DetailedStackStatus detailedStatus) {
        super(selector, stackId, exception);
        this.detailedStatus = detailedStatus;
    }

}
