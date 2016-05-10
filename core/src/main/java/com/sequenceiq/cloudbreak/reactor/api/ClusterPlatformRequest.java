package com.sequenceiq.cloudbreak.reactor.api;

import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.cloud.event.Selectable;

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
        return selector(getClass());
    }

    public static String selector(Class clazz) {
        return clazz.getSimpleName().toUpperCase();
    }
}
