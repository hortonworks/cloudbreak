package com.sequenceiq.flow.reactor.api.event;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.common.event.ResourceCrnPayload;
import com.sequenceiq.cloudbreak.common.event.Selectable;

import reactor.rx.Promise;

public class BaseFlowEvent implements Selectable, Acceptable, ResourceCrnPayload {
    private final String selector;

    private final Long resourceId;

    private final String resourceCrn;

    private final Promise<AcceptResult> accepted;

    public BaseFlowEvent(String selector, Long resourceId, String resourceCrn) {
        this(selector, resourceId, resourceCrn, new Promise<>());
    }

    public BaseFlowEvent(String selector, Long resourceId, String resourceCrn, Promise<AcceptResult> accepted) {
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
}
