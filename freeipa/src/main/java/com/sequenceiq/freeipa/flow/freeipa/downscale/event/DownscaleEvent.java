package com.sequenceiq.freeipa.flow.freeipa.downscale.event;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

import reactor.rx.Promise;

public class DownscaleEvent extends StackEvent {

    private final List<String> instanceIds;

    private final boolean repair;

    private final boolean chained;

    private final boolean finalChain;

    private final String operationId;

    private final int instanceCountByGroup;

    public DownscaleEvent(String selector, Long stackId, List<String> instanceIds, int instanceCountByGroup, boolean repair, boolean chained,
            boolean finalChain, String operationId) {
        this(selector, stackId, instanceIds, instanceCountByGroup, repair, chained, finalChain, operationId, new Promise<>());
    }

    @SuppressWarnings("ExecutableStatementCount")
    @JsonCreator
    public DownscaleEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("instanceIds") List<String> instanceIds,
            @JsonProperty("instanceCountByGroup") int instanceCountByGroup,
            @JsonProperty("repair") boolean repair,
            @JsonProperty("chained") boolean chained,
            @JsonProperty("finalChain") boolean finalChain,
            @JsonProperty("operationId") String operationId,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
        this.instanceIds = instanceIds;
        this.instanceCountByGroup = instanceCountByGroup;
        this.repair = repair;
        this.operationId = operationId;
        this.chained = chained;
        this.finalChain = finalChain;
    }

    public List<String> getInstanceIds() {
        return instanceIds;
    }

    public boolean isRepair() {
        return repair;
    }

    public int getInstanceCountByGroup() {
        return instanceCountByGroup;
    }

    public String getOperationId() {
        return operationId;
    }

    public boolean isChained() {
        return chained;
    }

    public boolean isFinalChain() {
        return finalChain;
    }

    @Override
    public boolean equalsEvent(StackEvent other) {
        return isClassAndEqualsEvent(DownscaleEvent.class, other,
                event -> Objects.equals(operationId, event.operationId)
                        && Objects.equals(instanceIds, event.instanceIds)
                        && instanceCountByGroup == event.instanceCountByGroup
                        && repair == event.repair
                        && chained == event.chained
                        && finalChain == event.finalChain);
    }

    @Override
    public String toString() {
        return "DownscaleEvent{" +
                "instanceIds=" + instanceIds +
                ", repair=" + repair +
                ", chained=" + chained +
                ", finalChain=" + finalChain +
                ", operationId='" + operationId + '\'' +
                ", instanceCountByGroup=" + instanceCountByGroup +
                "} " + super.toString();
    }
}
