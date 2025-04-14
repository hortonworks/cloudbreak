package com.sequenceiq.freeipa.flow.stack.dynamicentitlement;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;

public class RefreshEntitlementParamsFlowChainTriggerEvent extends BaseFlowEvent {

    private final Map<String, Boolean> changedEntitlements;

    private final Boolean saltRefreshNeeded;

    private final String operationId;

    @JsonCreator
    public RefreshEntitlementParamsFlowChainTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("operationId") String operationId,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("changedEntitlements") Map<String, Boolean> changedEntitlements,
            @JsonProperty("saltRefreshNeeded") Boolean saltRefreshNeeded) {
        super(selector, stackId, resourceCrn);
        this.changedEntitlements = changedEntitlements;
        this.saltRefreshNeeded = saltRefreshNeeded;
        this.operationId = operationId;
    }

    public Map<String, Boolean> getChangedEntitlements() {
        return changedEntitlements;
    }

    public Boolean getSaltRefreshNeeded() {
        return saltRefreshNeeded;
    }

    public String getOperationId() {
        return operationId;
    }

    @Override
    public boolean equalsEvent(BaseFlowEvent other) {
        return isClassAndEqualsEvent(RefreshEntitlementParamsFlowChainTriggerEvent.class, other,
                event -> Objects.equals(operationId, event.operationId) &&
                        Objects.equals(changedEntitlements, event.changedEntitlements) &&
                        Objects.equals(saltRefreshNeeded, event.saltRefreshNeeded));
    }

    @Override
    public String toString() {
        return "RefreshEntitlementParamsFlowChainTriggerEvent{" +
                "operationId='" + operationId + '\'' +
                ", changedEntitlements=" + changedEntitlements +
                ", saltRefreshNeeded=" + saltRefreshNeeded +
                ", resourceId=" + getResourceId() +
                "} " + super.toString();
    }
}
