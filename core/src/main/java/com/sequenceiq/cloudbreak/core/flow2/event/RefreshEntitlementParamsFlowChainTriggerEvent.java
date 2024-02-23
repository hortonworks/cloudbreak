package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;

public class RefreshEntitlementParamsFlowChainTriggerEvent extends BaseFlowEvent {
    private final Map<String, Boolean> changedEntitlements;

    private final Boolean saltRefreshNeeded;

    @JsonCreator
    public RefreshEntitlementParamsFlowChainTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("changedEntitlements") Map<String, Boolean> changedEntitlements,
            @JsonProperty("saltRefreshNeeded") Boolean saltRefreshNeeded) {
        super(selector, resourceId, resourceCrn);
        this.changedEntitlements = changedEntitlements;
        this.saltRefreshNeeded = saltRefreshNeeded;
    }

    public Map<String, Boolean> getChangedEntitlements() {
        return changedEntitlements;
    }

    public Boolean getSaltRefreshNeeded() {
        return saltRefreshNeeded;
    }

    @Override
    public boolean equalsEvent(BaseFlowEvent other) {
        return isClassAndEqualsEvent(RefreshEntitlementParamsFlowChainTriggerEvent.class, other,
                event -> Objects.equals(changedEntitlements, event.changedEntitlements) &&
                        Objects.equals(saltRefreshNeeded, event.saltRefreshNeeded));
    }

    @Override
    public String toString() {
        return "RefreshEntitlementParamsFlowChainTriggerEvent{" +
                "changedEntitlements=" + changedEntitlements +
                ", saltRefreshNeeded=" + saltRefreshNeeded +
                ", resourceId=" + getResourceId() +
                ", resourceCrn='" + getResourceCrn() +
                '}';
    }
}
