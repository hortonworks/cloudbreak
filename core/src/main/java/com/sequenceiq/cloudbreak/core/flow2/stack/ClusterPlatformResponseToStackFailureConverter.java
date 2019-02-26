package com.sequenceiq.cloudbreak.core.flow2.stack;

import com.sequenceiq.cloudbreak.core.flow2.PayloadConverter;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class ClusterPlatformResponseToStackFailureConverter implements PayloadConverter<StackFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return ClusterPlatformResult.class.isAssignableFrom(sourceClass);
    }

    @Override
    public StackFailureEvent convert(Object payload) {
        ClusterPlatformResult<?> clusterPlatformResult = (ClusterPlatformResult<?>) payload;
        return new StackFailureEvent(clusterPlatformResult.getRequest().getStackId(), clusterPlatformResult.getErrorDetails());
    }
}
