package com.sequenceiq.cloudbreak.core.flow2.stack;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.core.flow2.PayloadConverter;

public class CloudPlatformResponseToFlowFailureConverter implements PayloadConverter<FlowFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return CloudPlatformResult.class.isAssignableFrom(sourceClass);
    }

    @Override
    public FlowFailureEvent convert(Object payload) {
        CloudPlatformResult cloudPlatformResult = (CloudPlatformResult) payload;
        return new FlowFailureEvent(cloudPlatformResult.getRequest().getCloudContext().getId(), cloudPlatformResult.getErrorDetails());
    }
}
