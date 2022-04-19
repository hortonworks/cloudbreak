package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterRepairTriggerEvent extends StackEvent {

    private final Map<String, List<String>> failedNodesMap;

    private final boolean oneNodeFromEachHostGroupAtOnce;

    private final boolean restartServices;

    private final boolean upgrade;

    private final Long stackId;

    private final String triggeredStackVariant;

    public ClusterRepairTriggerEvent(Long stackId, Map<String, List<String>> failedNodesMap, boolean restartServices) {
        super(stackId);
        this.failedNodesMap = copyToSerializableMap(failedNodesMap);
        this.stackId = stackId;
        this.oneNodeFromEachHostGroupAtOnce = false;
        this.restartServices = restartServices;
        this.upgrade = false;
        this.triggeredStackVariant = null;
    }

    public ClusterRepairTriggerEvent(Long stackId, Map<String, List<String>> failedNodesMap, boolean oneNodeFromEachHostGroupAtOnce, boolean restartServices) {
        super(stackId);
        this.failedNodesMap = copyToSerializableMap(failedNodesMap);
        this.stackId = stackId;
        this.oneNodeFromEachHostGroupAtOnce = oneNodeFromEachHostGroupAtOnce;
        this.restartServices = restartServices;
        this.upgrade = false;
        this.triggeredStackVariant = null;
    }

    public ClusterRepairTriggerEvent(String event, Long stackId, Map<String, List<String>> failedNodesMap, boolean restartServices,
            String triggeredStackVariant) {
        super(event, stackId);
        this.failedNodesMap = copyToSerializableMap(failedNodesMap);
        this.stackId = stackId;
        this.oneNodeFromEachHostGroupAtOnce = false;
        this.restartServices = restartServices;
        this.upgrade = true;
        this.triggeredStackVariant = triggeredStackVariant;
    }

    public Map<String, List<String>> getFailedNodesMap() {
        return failedNodesMap;
    }

    public boolean isRestartServices() {
        return restartServices;
    }

    public boolean isUpgrade() {
        return upgrade;
    }

    public Long getStackId() {
        return stackId;
    }

    public String getTriggeredStackVariant() {
        return triggeredStackVariant;
    }

    public boolean isOneNodeFromEachHostGroupAtOnce() {
        return oneNodeFromEachHostGroupAtOnce;
    }

    private Map<String, List<String>> copyToSerializableMap(Map<String, List<String>> map) {
        Map<String, List<String>> result = new HashMap<>();
        map.forEach((key, value) -> result.put(key, new ArrayList<>(value)));
        return result;
    }

    @Override
    public boolean equalsEvent(StackEvent other) {
        return isClassAndEqualsEvent(ClusterRepairTriggerEvent.class, other,
                event -> restartServices == event.restartServices
                        && Objects.equals(failedNodesMap, event.failedNodesMap)
                        && Objects.equals(stackId, event.stackId));
    }
}
