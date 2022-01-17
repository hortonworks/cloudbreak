package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;

import reactor.rx.Promise;

public class ClusterDownscaleTriggerEvent extends ClusterScaleTriggerEvent {

    private final ClusterDownscaleDetails details;

    public ClusterDownscaleTriggerEvent(String selector, Long stackId, Map<String, Integer> hostGroupWithAdjustment,
            Map<String, Set<Long>> hostGroupWithPrivateIds, Map<String, Set<String>> hostGroupWithHostNames) {
        super(selector, stackId, hostGroupWithAdjustment, hostGroupWithPrivateIds, hostGroupWithHostNames);
        details = null;
    }

    public ClusterDownscaleTriggerEvent(String selector, Long stackId, Map<String, Integer> hostGroupWithAdjustment,
            Map<String, Set<Long>> hostGroupWithPrivateIds, Map<String, Set<String>> hostGroupWithHostNames, Promise<AcceptResult> accepted,
            ClusterDownscaleDetails details) {
        super(selector, stackId, hostGroupWithAdjustment, hostGroupWithPrivateIds, hostGroupWithHostNames, accepted);
        this.details = details;
    }

    public ClusterDownscaleDetails getDetails() {
        return details;
    }
}
