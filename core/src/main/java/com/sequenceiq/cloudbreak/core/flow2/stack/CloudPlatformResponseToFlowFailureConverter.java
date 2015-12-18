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
        return new FlowFailureEvent(((CloudPlatformResult) payload).getRequest().getCloudContext().getId(), ((CloudPlatformResult) payload).getErrorDetails());
    }
}
