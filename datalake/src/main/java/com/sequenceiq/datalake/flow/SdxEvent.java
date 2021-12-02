package com.sequenceiq.datalake.flow;

import java.util.Objects;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.event.IdempotentEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

import reactor.rx.Promise;

public class SdxEvent implements IdempotentEvent<SdxEvent> {
    private final String selector;

    private final Long sdxId;

    private final String sdxName;

    private final String userId;

    private final Promise<AcceptResult> accepted;

    public SdxEvent(Long sdxId, String userId) {
        this(null, sdxId, userId);
    }

    public SdxEvent(SdxContext context) {
        this(null, context.getSdxId(), context.getUserId());
    }

    public SdxEvent(String selector, Long sdxId, String userId) {
        this.selector = selector;
        this.sdxId = sdxId;
        this.userId = userId;
        accepted = new Promise<>();
        sdxName = null;
    }

    public SdxEvent(String selector, Long sdxId, String sdxName, String userId) {
        this.selector = selector;
        this.sdxId = sdxId;
        this.sdxName = sdxName;
        this.userId = userId;
        accepted = new Promise<>();
    }

    public SdxEvent(String selector, SdxContext context) {
        this(selector, context.getSdxId(), context.getUserId());
    }

    public SdxEvent(String selector, Long sdxId, String userId, Promise<AcceptResult> accepted) {
        this.selector = selector;
        this.sdxId = sdxId;
        this.userId = userId;
        this.accepted = accepted;
        this.sdxName = null;
    }

    @Override
    public Long getResourceId() {
        return sdxId;
    }

    public String getUserId() {
        return userId;
    }

    @Override
    public String selector() {
        return StringUtils.isNotEmpty(selector) ? selector : EventSelectorUtil.selector(getClass());
    }

    public String getSdxName() {
        return sdxName;
    }

    @Override
    public Promise<AcceptResult> accepted() {
        return accepted;
    }

    @Override
    public boolean equalsEvent(SdxEvent other) {
        return isClassAndEqualsEvent(SdxEvent.class, other);
    }

    protected <T extends SdxEvent> boolean isClassAndEqualsEvent(Class<T> clazz, SdxEvent other) {
        return isClassAndEqualsEvent(clazz, other, sdxEvent -> true);
    }

    protected <T extends SdxEvent> boolean isClassAndEqualsEvent(Class<T> clazz, SdxEvent other, Predicate<T> equalsSubclass) {
        if (!clazz.equals(getClass())) {
            return false;
        }
        return Objects.equals(selector, other.selector)
                && Objects.equals(sdxId, other.sdxId)
                && equalsSubclass.test((T) other);
    }
}
