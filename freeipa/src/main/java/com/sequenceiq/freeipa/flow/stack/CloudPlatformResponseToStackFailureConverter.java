package com.sequenceiq.freeipa.flow.stack;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.flow.core.PayloadConverter;

public class CloudPlatformResponseToStackFailureConverter implements PayloadConverter<StackFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return CloudPlatformResult.class.isAssignableFrom(sourceClass);
    }

    @Override
    public StackFailureEvent convert(Object payload) {
        CloudPlatformResult cloudPlatformResult = (CloudPlatformResult) payload;
        return new StackFailureEvent(cloudPlatformResult.getResourceId(), cloudPlatformResult.getErrorDetails(), ERROR);
    }
}
