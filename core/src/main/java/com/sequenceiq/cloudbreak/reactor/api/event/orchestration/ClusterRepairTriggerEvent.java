package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterRepairTriggerEvent extends StackEvent {

    private final Map<String, List<String>> failedNodesMap;

    private final boolean removeOnly;

    private final boolean restartServices;

    private final Long stackId;

    public ClusterRepairTriggerEvent(Long stackId, Map<String, List<String>> failedNodesMap, boolean removeOnly, boolean restartServices) {
        super(stackId);
        this.failedNodesMap = failedNodesMap;
        this.removeOnly = removeOnly;
        this.stackId = stackId;
        this.restartServices = restartServices;
    }

    public Map<String, List<String>> getFailedNodesMap() {
        return failedNodesMap;
    }

    public boolean isRemoveOnly() {
        return removeOnly;
    }

    public boolean isRestartServices() {
        return restartServices;
    }

    public Long getStackId() {
        return stackId;
    }
}
