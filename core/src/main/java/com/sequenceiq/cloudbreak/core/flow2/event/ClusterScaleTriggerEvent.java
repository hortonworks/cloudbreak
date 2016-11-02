package com.sequenceiq.cloudbreak.core.flow2.event;

import com.sequenceiq.cloudbreak.reactor.api.event.HostGroupPayload;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterScaleTriggerEvent extends StackEvent implements HostGroupPayload {
    private final String hostGroup;

    private final Integer adjustment;

    public ClusterScaleTriggerEvent(String selector, Long stackId, String hostGroup, Integer adjustment) {
        super(selector, stackId);
        this.hostGroup = hostGroup;
        this.adjustment = adjustment;
    }

    public String getHostGroupName() {
        return hostGroup;
    }

    public Integer getAdjustment() {
        return adjustment;
    }
}
