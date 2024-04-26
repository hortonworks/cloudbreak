package com.sequenceiq.freeipa.flow.stack.dynamicentitlement;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class RefreshEntitlementParamsTriggerEvent extends StackEvent {

    private final Map<String, Boolean> changedEntitlements;

    private final boolean chained;

    private final boolean finalChain;

    private final String operationId;

    @JsonCreator
    public RefreshEntitlementParamsTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("changedEntitlements") Map<String, Boolean> changedEntitlements,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted,
            @JsonProperty("chained") boolean chained,
            @JsonProperty("finalChain") boolean finalChain,
            @JsonProperty("operationId") String operationId) {
        super(selector, resourceId, accepted);
        this.changedEntitlements = changedEntitlements;
        this.chained = chained;
        this.finalChain = finalChain;
        this.operationId = operationId;
    }

    public static RefreshEntitlementParamsTriggerEvent fromChainTrigger(RefreshEntitlementParamsFlowChainTriggerEvent chainTriggerEvent,
            boolean chained, boolean finalChain) {
        return new RefreshEntitlementParamsTriggerEvent(
                EventSelectorUtil.selector(RefreshEntitlementParamsTriggerEvent.class),
                chainTriggerEvent.getResourceId(),
                chainTriggerEvent.getChangedEntitlements(),
                chainTriggerEvent.accepted(),
                chained,
                finalChain,
                chainTriggerEvent.getOperationId());
    }

    public Map<String, Boolean> getChangedEntitlements() {
        return changedEntitlements;
    }

    public boolean isChained() {
        return chained;
    }

    public boolean isFinalChain() {
        return finalChain;
    }

    public String getOperationId() {
        return operationId;
    }

    @Override
    public boolean equalsEvent(StackEvent other) {
        return isClassAndEqualsEvent(RefreshEntitlementParamsTriggerEvent.class, other,
                event -> Objects.equals(changedEntitlements, event.changedEntitlements) &&
                        Objects.equals(operationId, event.operationId));
    }

    @Override
    public String toString() {
        return "RefreshEntitlementParamsTriggerEvent{" +
                "chained=" + chained +
                ", finalChain=" + finalChain +
                ", operationId='" + operationId + '\'' +
                ", changedEntitlements=" + changedEntitlements +
                "} " + super.toString();
    }
}
