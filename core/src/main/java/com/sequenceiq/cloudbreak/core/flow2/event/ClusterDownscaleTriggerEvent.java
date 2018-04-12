package com.sequenceiq.cloudbreak.core.flow2.event;

import reactor.rx.Promise;

import java.util.Set;

public class ClusterDownscaleTriggerEvent extends ClusterScaleTriggerEvent {
    private final Set<String> hostNames;

    public ClusterDownscaleTriggerEvent(String selector, Long stackId, String hostGroup, Integer adjustment) {
        super(selector, stackId, hostGroup, adjustment);
        hostNames = null;
    }

    public ClusterDownscaleTriggerEvent(String selector, Long stackId, String hostGroup, Integer adjustment, Promise<Boolean> accepted) {
        super(selector, stackId, hostGroup, adjustment, accepted);
        hostNames = null;
    }

    public ClusterDownscaleTriggerEvent(String selector, Long stackId, String hostGroup, Set<String> hostNames) {
        super(selector, stackId, hostGroup, null);
        this.hostNames = hostNames;
    }

    public ClusterDownscaleTriggerEvent(String selector, Long stackId, String hostGroup, Set<String> hostNames, Promise<Boolean> accepted) {
        super(selector, stackId, hostGroup, null, accepted);
        this.hostNames = hostNames;
    }

    public Set<String> getHostNames() {
        return hostNames;
    }
}
