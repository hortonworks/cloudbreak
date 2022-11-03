package com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class ChangePrimaryGatewayEvent extends StackEvent {

    private final String operationId;

    private final Boolean finalChain;

    private final List<String> repairInstanceIds;

    public ChangePrimaryGatewayEvent(String selector, Long stackId, List<String> repairInstanceIds, Boolean finalChain, String operationId) {
        super(selector, stackId);
        this.repairInstanceIds = repairInstanceIds;
        this.finalChain = finalChain;
        this.operationId = operationId;
    }

    @JsonCreator
    public ChangePrimaryGatewayEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("repairInstanceIds") List<String> repairInstanceIds,
            @JsonProperty("finalChain") Boolean finalChain,
            @JsonProperty("operationId") String operationId,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
        this.repairInstanceIds = repairInstanceIds;
        this.finalChain = finalChain;
        this.operationId = operationId;
    }

    public List<String> getRepairInstanceIds() {
        return repairInstanceIds;
    }

    public Boolean getFinalChain() {
        return finalChain;
    }

    public String getOperationId() {
        return operationId;
    }

    @Override
    public boolean equalsEvent(StackEvent other) {
        return isClassAndEqualsEvent(ChangePrimaryGatewayEvent.class, other,
                event -> Objects.equals(operationId, event.operationId)
                        && Objects.equals(repairInstanceIds, event.repairInstanceIds)
                        && Objects.equals(finalChain, event.finalChain));
    }

    @Override
    public String toString() {
        return "ChangePrimaryGatewayEvent{"
                + "stackId=" + getResourceId()
                + ", repairInstanceIds='" + repairInstanceIds + '\''
                + ", finalChain='" + finalChain + '\''
                + ", operationId='" + operationId + '\''
                + "}";
    }
}
