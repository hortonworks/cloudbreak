package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base;

import java.util.Optional;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceLifeCycle;

public enum InstanceLifeCycle {
    NORMAL,
    SPOT;

    public static InstanceLifeCycle getDefault() {
        return NORMAL;
    }

    public static InstanceLifeCycle fromCloudInstanceLifeCycle(CloudInstanceLifeCycle cloudInstanceLifeCycle) {
        return Optional.ofNullable(cloudInstanceLifeCycle)
                .map(CloudInstanceLifeCycle::name)
                .map(InstanceLifeCycle::valueOf)
                .orElse(getDefault());
    }
}
