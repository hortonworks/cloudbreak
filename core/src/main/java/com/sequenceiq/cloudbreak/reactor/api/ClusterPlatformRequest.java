package com.sequenceiq.cloudbreak.reactor.api;

import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;

public abstract class ClusterPlatformRequest implements Payload, Selectable {

    private final Long stackId;

    public ClusterPlatformRequest(Long stackId) {
        this.stackId = stackId;
    }

    @Override
    public Long getStackId() {
        return stackId;
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(getClass());
    }
}
