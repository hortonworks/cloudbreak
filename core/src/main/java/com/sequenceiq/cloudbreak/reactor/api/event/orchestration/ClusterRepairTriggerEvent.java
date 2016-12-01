package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterRepairTriggerEvent extends StackEvent {

    private final Map<String, List<String>> failedNodesMap;

    public ClusterRepairTriggerEvent(Long stackId, Map<String, List<String>> failedNodesMap) {
        super(stackId);
        this.failedNodesMap = failedNodesMap;
    }

    public Map<String, List<String>> getFailedNodesMap() {
        return failedNodesMap;
    }
}
