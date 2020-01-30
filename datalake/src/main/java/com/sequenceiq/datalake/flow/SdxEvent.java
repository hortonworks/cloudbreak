package com.sequenceiq.datalake.flow;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.event.EventSelectorUtil;

import reactor.rx.Promise;

public class SdxEvent implements Selectable, Acceptable {
    private final String selector;

    private final Long sdxId;

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
    }

    public SdxEvent(String selector, SdxContext context) {
        this(selector, context.getSdxId(), context.getUserId());
    }

    public SdxEvent(String selector, Long sdxId, String userId, Promise<AcceptResult> accepted) {
        this.selector = selector;
        this.sdxId = sdxId;
        this.userId = userId;
        this.accepted = accepted;
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

    @Override
    public Promise<AcceptResult> accepted() {
        return accepted;
    }

}
