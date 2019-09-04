package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Set;

import reactor.rx.Promise;

public class ClusterDownscaleTriggerEvent extends ClusterScaleTriggerEvent {
    private final Set<Long> privateIds;

    private final ClusterDownscaleDetails details;

    public ClusterDownscaleTriggerEvent(String selector, Long stackId, String hostGroup, Integer adjustment) {
        super(selector, stackId, hostGroup, adjustment);
        details = null;
        privateIds = null;
    }

    public ClusterDownscaleTriggerEvent(String selector, Long stackId, String hostGroup, Integer adjustment,  ClusterDownscaleDetails details) {
        super(selector, stackId, hostGroup, adjustment);
        this.details = details;
        privateIds = null;
    }

    public ClusterDownscaleTriggerEvent(String selector, Long stackId, String hostGroup, Integer adjustment, Promise<Boolean> accepted,
            ClusterDownscaleDetails details) {
        super(selector, stackId, hostGroup, adjustment, accepted);
        this.details = details;
        privateIds = null;
    }

    public ClusterDownscaleTriggerEvent(String selector, Long stackId, String hostGroup, Set<Long> privateIds) {
        super(selector, stackId, hostGroup, null);
        details = null;
        this.privateIds = privateIds;
    }

    public ClusterDownscaleTriggerEvent(String selector, Long stackId, String hostGroup, Set<Long> privateIds, Promise<Boolean> accepted,
            ClusterDownscaleDetails details) {
        super(selector, stackId, hostGroup, null, accepted);
        this.details = details;
        this.privateIds = privateIds;
    }

    public Set<Long> getPrivateIds() {
        return privateIds;
    }

    public ClusterDownscaleDetails getDetails() {
        return details;
    }
}
