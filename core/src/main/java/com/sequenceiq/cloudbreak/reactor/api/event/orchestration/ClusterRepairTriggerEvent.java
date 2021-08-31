package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import java.util.ArrayList;
import java.util.HashMap;
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
        this.failedNodesMap = copyToSerializableMap(failedNodesMap);
        this.removeOnly = removeOnly;
        this.stackId = stackId;
        this.restartServices = restartServices;
    }

    public ClusterRepairTriggerEvent(String event, Long stackId, Map<String, List<String>> failedNodesMap, boolean removeOnly, boolean restartServices) {
        super(event, stackId);
        this.failedNodesMap = copyToSerializableMap(failedNodesMap);
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

    private Map<String, List<String>> copyToSerializableMap(Map<String, List<String>> map) {
        Map<String, List<String>> result = new HashMap<>();
        map.forEach((key, value) -> result.put(key, new ArrayList<>(value)));
        return result;
    }
}
