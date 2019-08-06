package com.sequenceiq.datalake.flow;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.event.EventSelectorUtil;

import reactor.rx.Promise;

public class SdxEvent implements Selectable, Acceptable {
    private final String selector;

    private final Long sdxId;

    private final String userId;

    private final String requestId;

    private final Promise<Boolean> accepted;

    public SdxEvent(Long sdxId, String userId, String requestId) {
        this(null, sdxId, userId, requestId);
    }

    public SdxEvent(String selector, Long sdxId, String userId, String requestId) {
        this.selector = selector;
        this.sdxId = sdxId;
        this.userId = userId;
        this.requestId = requestId;
        accepted = new Promise<>();
    }

    public SdxEvent(String selector, Long sdxId, String userId, String requestId, Promise<Boolean> accepted) {
        this.selector = selector;
        this.sdxId = sdxId;
        this.userId = userId;
        this.requestId = requestId;
        this.accepted = accepted;
    }

    @Override
    public Long getResourceId() {
        return sdxId;
    }

    public String getUserId() {
        return userId;
    }

    public String getRequestId() {
        return requestId;
    }

    @Override
    public String selector() {
        return StringUtils.isNotEmpty(selector) ? selector : EventSelectorUtil.selector(getClass());
    }

    @Override
    public Promise<Boolean> accepted() {
        return accepted;
    }

}
