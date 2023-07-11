package com.sequenceiq.cloudbreak.reactor.api;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.event.EventSelectorUtil;

public abstract class ClusterPlatformRequest implements Selectable {

    private final Long stackId;

    private boolean datahubRefreshNeeded;

    protected ClusterPlatformRequest(Long stackId) {
        this.stackId = stackId;
    }

    @Override
    public Long getResourceId() {
        return stackId;
    }

    public Long getStackId() {
        return stackId;
    }

    public boolean isDatahubRefreshNeeded() {
        return datahubRefreshNeeded;
    }

    public void setDatahubRefreshNeeded(boolean datahubRefreshNeeded) {
        this.datahubRefreshNeeded = datahubRefreshNeeded;
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(getClass());
    }

    @Override
    public String toString() {
        return "ClusterPlatformRequest{" +
                "stackId=" + stackId +
                '}';
    }
}
