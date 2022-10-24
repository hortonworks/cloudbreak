package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterUpgradeS3guardValidationFinishedEvent  extends StackEvent {
    public ClusterUpgradeS3guardValidationFinishedEvent(String selector, Long stackId) {
        super(selector, stackId);
    }
}
