package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterRepairTriggerEvent extends StackEvent {

    private final Map<String, List<String>> failedNodesMap;

    private final RepairType repairType;

    private final boolean restartServices;

    private final boolean upgrade;

    private final Long stackId;

    private final String triggeredStackVariant;

    private final boolean rollingRestartEnabled;

    public ClusterRepairTriggerEvent(Long stackId, Map<String, List<String>> failedNodesMap, RepairType repairType, boolean restartServices,
            String triggeredStackVariant) {
        super(stackId);
        this.failedNodesMap = copyToSerializableMap(failedNodesMap);
        this.stackId = stackId;
        this.repairType = repairType;
        this.restartServices = restartServices;
        this.upgrade = triggeredStackVariant != null;
        this.triggeredStackVariant = triggeredStackVariant;
        this.rollingRestartEnabled = false;
    }

    public ClusterRepairTriggerEvent(Long stackId, Map<String, List<String>> failedNodesMap, RepairType repairType, boolean restartServices,
            String triggeredStackVariant, boolean upgrade) {
        super(stackId);
        this.failedNodesMap = copyToSerializableMap(failedNodesMap);
        this.stackId = stackId;
        this.repairType = repairType;
        this.restartServices = restartServices;
        this.upgrade = upgrade;
        this.triggeredStackVariant = triggeredStackVariant;
        this.rollingRestartEnabled = false;
    }

    @JsonCreator
    public ClusterRepairTriggerEvent(
            @JsonProperty("selector") String event,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("repairType") RepairType repairType,
            @JsonProperty("failedNodesMap") Map<String, List<String>> failedNodesMap,
            @JsonProperty("restartServices") boolean restartServices,
            @JsonProperty("triggeredStackVariant") String triggeredStackVariant,
            @JsonProperty("rollingRestartEnabled") boolean rollingRestartEnabled) {
        super(event, stackId);
        this.failedNodesMap = copyToSerializableMap(failedNodesMap);
        this.stackId = stackId;
        this.repairType = repairType;
        this.restartServices = restartServices;
        this.upgrade = triggeredStackVariant != null;
        this.triggeredStackVariant = triggeredStackVariant;
        this.rollingRestartEnabled = rollingRestartEnabled;
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

    public RepairType getRepairType() {
        return repairType;
    }

    public boolean isRollingRestartEnabled() {
        return rollingRestartEnabled;
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

    public enum RepairType {
        ALL_AT_ONCE,
        BATCH,
        ONE_FROM_EACH_HOSTGROUP,
        ONE_BY_ONE
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ClusterRepairTriggerEvent.class.getSimpleName() + "[", "]")
                .add("failedNodesMap=" + failedNodesMap)
                .add("repairType=" + repairType)
                .add("restartServices=" + restartServices)
                .add("upgrade=" + upgrade)
                .add("stackId=" + stackId)
                .add("triggeredStackVariant='" + triggeredStackVariant + "'")
                .add("rollingRestartEnabled=" + rollingRestartEnabled)
                .add(super.toString())
                .toString();
    }
}
