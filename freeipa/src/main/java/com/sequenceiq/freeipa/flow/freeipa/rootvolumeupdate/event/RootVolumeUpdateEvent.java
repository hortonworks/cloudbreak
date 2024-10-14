package com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.event;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class RootVolumeUpdateEvent extends StackEvent {

    private final String operationId;

    private final int instanceCountByGroup;

    private final List<String> updateInstanceIds;

    private final String pgwInstanceId;

    @JsonCreator
    public RootVolumeUpdateEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("operationId") String operationId,
            @JsonProperty("instanceCountByGroup") int instanceCountByGroup,
            @JsonProperty("updateInstanceIds") List<String> updateInstanceIds,
            @JsonProperty("pgwInstanceId") String pgwInstanceId) {
        super(selector, stackId);
        this.operationId = operationId;
        this.instanceCountByGroup = instanceCountByGroup;
        this.updateInstanceIds = updateInstanceIds;
        this.pgwInstanceId = pgwInstanceId;
    }

    public String getOperationId() {
        return operationId;
    }

    public int getInstanceCountByGroup() {
        return instanceCountByGroup;
    }

    public List<String> getUpdateInstanceIds() {
        return updateInstanceIds;
    }

    public String getPgwInstanceId() {
        return pgwInstanceId;
    }

    @Override
    public boolean equalsEvent(StackEvent other) {
        return isClassAndEqualsEvent(RootVolumeUpdateEvent.class, other,
                event -> Objects.equals(operationId, event.operationId)
                        && instanceCountByGroup == event.instanceCountByGroup
                        && Objects.equals(updateInstanceIds, event.updateInstanceIds)
                        && Objects.equals(pgwInstanceId, event.pgwInstanceId));
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", RootVolumeUpdateEvent.class.getSimpleName() + "[", "]")
                .add("operationId=" + operationId)
                .add("instanceCountByGroup=" + instanceCountByGroup)
                .add("updateInstanceIds=" + updateInstanceIds)
                .add("pgwInstanceId=" + pgwInstanceId)
                .toString();
    }
}
