package com.sequenceiq.cloudbreak.core.flow2.event;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class AwsVariantMigrationTriggerEvent extends StackEvent {

    private final String hostGroupName;

    public AwsVariantMigrationTriggerEvent(String selector, Long stackId, String hostGroupName) {
        super(selector, stackId);
        this.hostGroupName = hostGroupName;
    }

    public String getHostGroupName() {
        return hostGroupName;
    }
}
