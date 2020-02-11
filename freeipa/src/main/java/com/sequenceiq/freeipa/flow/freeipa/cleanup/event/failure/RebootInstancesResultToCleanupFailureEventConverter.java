package com.sequenceiq.freeipa.flow.freeipa.cleanup.event.failure;

import com.sequenceiq.cloudbreak.cloud.event.instance.RebootInstancesResult;
import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.instance.InstanceFailureEvent;

public class RebootInstancesResultToCleanupFailureEventConverter implements PayloadConverter<InstanceFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return RebootInstancesResult.class.isAssignableFrom(sourceClass);
    }

    @Override
    public InstanceFailureEvent convert(Object payload) {
        RebootInstancesResult result = (RebootInstancesResult) payload;
        return new InstanceFailureEvent(result.getResourceId(), result.getErrorDetails(), result.getInstanceIds());
    }
}
