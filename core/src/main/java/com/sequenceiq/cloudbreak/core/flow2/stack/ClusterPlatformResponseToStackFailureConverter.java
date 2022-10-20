package com.sequenceiq.cloudbreak.core.flow2.stack;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.flow.core.PayloadConverter;

public class ClusterPlatformResponseToStackFailureConverter implements PayloadConverter<StackFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return ClusterPlatformResult.class.isAssignableFrom(sourceClass);
    }

    @Override
    public StackFailureEvent convert(Object payload) {
        ClusterPlatformResult<?> clusterPlatformResult = (ClusterPlatformResult<?>) payload;
        return new StackFailureEvent(clusterPlatformResult.getRequest().getResourceId(), clusterPlatformResult.getErrorDetails());
    }
}
