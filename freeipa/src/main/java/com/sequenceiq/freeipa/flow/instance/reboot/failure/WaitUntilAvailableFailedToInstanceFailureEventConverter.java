package com.sequenceiq.freeipa.flow.instance.reboot.failure;

import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.instance.InstanceFailureEvent;
import com.sequenceiq.freeipa.flow.stack.HealthCheckFailed;

public class WaitUntilAvailableFailedToInstanceFailureEventConverter implements PayloadConverter<InstanceFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return HealthCheckFailed.class.isAssignableFrom(sourceClass);
    }

    @Override
    public InstanceFailureEvent convert(Object payload) {
        HealthCheckFailed result = (HealthCheckFailed) payload;
        return new InstanceFailureEvent(result.getResourceId(), result.getException(), result.getInstanceIds());
    }
}
