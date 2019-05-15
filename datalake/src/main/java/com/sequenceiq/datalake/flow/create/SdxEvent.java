package com.sequenceiq.datalake.flow.create;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.event.EventSelectorUtil;

import reactor.rx.Promise;

public class SdxEvent implements Selectable, Acceptable {
    private final String selector;

    private final Long sdxId;

    private final Promise<Boolean> accepted;

    public SdxEvent(Long sdxId) {
        this(null, sdxId);
    }

    public SdxEvent(String selector, Long sdxId) {
        this.selector = selector;
        this.sdxId = sdxId;
        accepted = new Promise<>();
    }

    public SdxEvent(String selector, Long sdxId, Promise<Boolean> accepted) {
        this.selector = selector;
        this.sdxId = sdxId;
        this.accepted = accepted;
    }

    @Override
    public Long getResourceId() {
        return sdxId;
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
