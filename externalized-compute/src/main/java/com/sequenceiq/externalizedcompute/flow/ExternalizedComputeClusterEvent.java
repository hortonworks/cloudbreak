package com.sequenceiq.externalizedcompute.flow;

import java.util.Objects;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.event.IdempotentEvent;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.flow.event.EventSelectorUtil;

public class ExternalizedComputeClusterEvent implements IdempotentEvent<ExternalizedComputeClusterEvent> {
    private final String selector;

    private final Long externalizedComputeId;

    private final String externalizedComputeName;

    private final String userId;

    private final Promise<AcceptResult> accepted;

    public ExternalizedComputeClusterEvent(Long externalizedComputeId, String userId) {
        this(null, externalizedComputeId, userId);
    }

    public ExternalizedComputeClusterEvent(ExternalizedComputeClusterContext context) {
        this(null, context.getExternalizedComputeId(), context.getUserId());
    }

    public ExternalizedComputeClusterEvent(String selector, Long externalizedComputeId, String userId) {
        this.selector = selector;
        this.externalizedComputeId = externalizedComputeId;
        this.userId = userId;
        accepted = new Promise<>();
        externalizedComputeName = null;
    }

    @JsonCreator
    public ExternalizedComputeClusterEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long externalizedComputeId,
            @JsonProperty("externalizedComputeName") String externalizedComputeName,
            @JsonProperty("userId") String userId) {
        this.selector = selector;
        this.externalizedComputeId = externalizedComputeId;
        this.externalizedComputeName = externalizedComputeName;
        this.userId = userId;
        accepted = new Promise<>();
    }

    public ExternalizedComputeClusterEvent(String selector, ExternalizedComputeClusterContext context) {
        this(selector, context.getExternalizedComputeId(), context.getUserId());
    }

    public ExternalizedComputeClusterEvent(String selector, Long externalizedComputeId, String userId, Promise<AcceptResult> accepted) {
        this.selector = selector;
        this.externalizedComputeId = externalizedComputeId;
        this.userId = userId;
        this.accepted = accepted;
        this.externalizedComputeName = null;
    }

    @Override
    public Long getResourceId() {
        return externalizedComputeId;
    }

    public String getUserId() {
        return userId;
    }

    @Override
    public String selector() {
        return StringUtils.isNotEmpty(selector) ? selector : EventSelectorUtil.selector(getClass());
    }

    public String getExternalizedComputeName() {
        return externalizedComputeName;
    }

    @Override
    public Promise<AcceptResult> accepted() {
        return accepted;
    }

    @Override
    public boolean equalsEvent(ExternalizedComputeClusterEvent other) {
        return isClassAndEqualsEvent(ExternalizedComputeClusterEvent.class, other);
    }

    protected <T extends ExternalizedComputeClusterEvent> boolean isClassAndEqualsEvent(Class<T> clazz, ExternalizedComputeClusterEvent other) {
        return isClassAndEqualsEvent(clazz, other, externalizedCompute -> true);
    }

    protected <T extends ExternalizedComputeClusterEvent> boolean isClassAndEqualsEvent(Class<T> clazz, ExternalizedComputeClusterEvent other,
            Predicate<T> equalsSubclass) {
        if (!clazz.equals(getClass())) {
            return false;
        }
        return Objects.equals(selector, other.selector)
                && Objects.equals(externalizedComputeId, other.externalizedComputeId)
                && equalsSubclass.test((T) other);
    }

    @Override
    public String toString() {
        return "ExternalizedComputeClusterEvent{" +
                "selector='" + selector + '\'' +
                ", externalizedComputeId=" + externalizedComputeId +
                ", externalizedComputeName='" + externalizedComputeName + '\'' +
                ", userId='" + userId + '\'' +
                ", accepted=" + accepted +
                '}';
    }
}
