package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Set;

import reactor.rx.Promise;

public class ClusterDownscaleTriggerEvent extends ClusterScaleTriggerEvent {
    private final Set<Long> privateIds;

    public ClusterDownscaleTriggerEvent(String selector, Long stackId, String hostGroup, Integer adjustment) {
        super(selector, stackId, hostGroup, adjustment);
        privateIds = null;
    }

    public ClusterDownscaleTriggerEvent(String selector, Long stackId, String hostGroup, Integer adjustment, Promise<Boolean> accepted) {
        super(selector, stackId, hostGroup, adjustment, accepted);
        privateIds = null;
    }

    public ClusterDownscaleTriggerEvent(String selector, Long stackId, String hostGroup, Set<Long> privateIds) {
        super(selector, stackId, hostGroup, null);
        this.privateIds = privateIds;
    }

    public ClusterDownscaleTriggerEvent(String selector, Long stackId, String hostGroup, Set<Long> privateIds, Promise<Boolean> accepted) {
        super(selector, stackId, hostGroup, null, accepted);
        this.privateIds = privateIds;
    }

    public Set<Long> getPrivateIds() {
        return privateIds;
    }
}
