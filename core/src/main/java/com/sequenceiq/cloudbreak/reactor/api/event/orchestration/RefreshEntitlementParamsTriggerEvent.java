package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.core.flow2.event.RefreshEntitlementParamsFlowChainTriggerEvent;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;

public class RefreshEntitlementParamsTriggerEvent extends BaseFlowEvent {

    private final Map<String, Boolean> changedEntitlements;

    @JsonCreator
    public RefreshEntitlementParamsTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonProperty("changedEntitlements") Map<String, Boolean> changedEntitlements,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {
        super(selector, resourceId, resourceCrn, accepted);
        this.changedEntitlements = changedEntitlements;
    }

    public static RefreshEntitlementParamsTriggerEvent fromChainTrigger(RefreshEntitlementParamsFlowChainTriggerEvent chainTriggerEvent) {
        return new RefreshEntitlementParamsTriggerEvent(
                EventSelectorUtil.selector(RefreshEntitlementParamsTriggerEvent.class),
                chainTriggerEvent.getResourceId(),
                chainTriggerEvent.getResourceCrn(),
                chainTriggerEvent.getChangedEntitlements(),
                chainTriggerEvent.accepted());
    }

    public Map<String, Boolean> getChangedEntitlements() {
        return changedEntitlements;
    }

    @Override
    public boolean equalsEvent(BaseFlowEvent other) {
        return isClassAndEqualsEvent(RefreshEntitlementParamsTriggerEvent.class, other,
                event -> Objects.equals(changedEntitlements, event.changedEntitlements));
    }

    @Override
    public String toString() {
        return "RefreshEntitlementParamsTriggerEvent{" +
                "changedEntitlements=" + changedEntitlements +
                '}';
    }
}
