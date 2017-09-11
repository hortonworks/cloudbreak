package com.sequenceiq.cloudbreak.core.flow2.event;

import com.sequenceiq.cloudbreak.reactor.api.event.HostGroupPayload;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

import reactor.rx.Promise;

public class ClusterScaleTriggerEvent extends StackEvent implements HostGroupPayload {
    private final String hostGroup;

    private final Integer adjustment;

    public ClusterScaleTriggerEvent(String selector, Long stackId, String hostGroup, Integer adjustment) {
        super(selector, stackId);
        this.hostGroup = hostGroup;
        this.adjustment = adjustment;
    }

    public ClusterScaleTriggerEvent(String selector, Long stackId, String hostGroup, Integer adjustment, Promise<Boolean> accepted) {
        super(selector, stackId, accepted);
        this.hostGroup = hostGroup;
        this.adjustment = adjustment;
    }

    @Override
    public String getHostGroupName() {
        return hostGroup;
    }

    public Integer getAdjustment() {
        return adjustment;
    }
}
