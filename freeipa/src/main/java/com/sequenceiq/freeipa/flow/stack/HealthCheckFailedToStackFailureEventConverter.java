package com.sequenceiq.freeipa.flow.stack;

import com.sequenceiq.flow.core.PayloadConverter;

public class HealthCheckFailedToStackFailureEventConverter implements PayloadConverter<StackFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return HealthCheckFailed.class.isAssignableFrom(sourceClass);
    }

    @Override
    public StackFailureEvent convert(Object payload) {
        HealthCheckFailed result = (HealthCheckFailed) payload;
        return new StackFailureEvent(result.getResourceId(), result.getException());
    }
}
