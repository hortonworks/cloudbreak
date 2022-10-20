package com.sequenceiq.cloudbreak.core.flow2.stack;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.flow.core.PayloadConverter;

public class CloudPlatformResponseToStackFailureConverter implements PayloadConverter<StackFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return CloudPlatformResult.class.isAssignableFrom(sourceClass);
    }

    @Override
    public StackFailureEvent convert(Object payload) {
        CloudPlatformResult cloudPlatformResult = (CloudPlatformResult) payload;
        return new StackFailureEvent(cloudPlatformResult.getResourceId(), cloudPlatformResult.getErrorDetails());
    }
}
