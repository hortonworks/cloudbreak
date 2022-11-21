package com.sequenceiq.flow.reactor.api.event;

import java.util.Objects;
import java.util.function.Predicate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.event.IdempotentEvent;
import com.sequenceiq.cloudbreak.common.event.ResourceCrnPayload;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;

public class BaseFlowEvent implements IdempotentEvent<BaseFlowEvent>, ResourceCrnPayload {

    private final String selector;

    private final Long resourceId;

    private final String resourceCrn;

    private final Promise<AcceptResult> accepted;

    public BaseFlowEvent(String selector, Long resourceId, String resourceCrn) {
        this(selector, resourceId, resourceCrn, new Promise<>());
    }

    @JsonCreator
    public BaseFlowEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("resourceCrn") String resourceCrn,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted) {

        this.selector = selector;
        this.resourceId = resourceId;
        this.resourceCrn = resourceCrn;
        this.accepted = accepted;
    }

    @Override
    public Long getResourceId() {
        return resourceId;
    }

    @Override
    public String selector() {
        return selector;
    }

    @Override
    public Promise<AcceptResult> accepted() {
        return accepted;
    }

    @Override
    public String getResourceCrn() {
        return resourceCrn;
    }

    @Override
    public boolean equalsEvent(BaseFlowEvent other) {
        return isClassAndEqualsEvent(BaseFlowEvent.class, other);
    }

    protected <T extends BaseFlowEvent> boolean isClassAndEqualsEvent(Class<T> clazz, BaseFlowEvent other) {
        return isClassAndEqualsEvent(clazz, other, baseFlowEvent -> true);
    }

    protected <T extends BaseFlowEvent> boolean isClassAndEqualsEvent(Class<T> clazz, BaseFlowEvent other, Predicate<T> equalsSubclass) {
        if (!clazz.equals(getClass())) {
            return false;
        }
        return Objects.equals(selector, other.selector)
                && Objects.equals(resourceId, other.resourceId)
                && Objects.equals(resourceCrn, other.resourceCrn)
                && equalsSubclass.test((T) other);
    }
}
