package com.sequenceiq.freeipa.flow.freeipa.upscale.failure;

import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.ValidateNewInstancesHealthFailedEvent;

public class ValidateNewInstancesHealthFailedToUpscaleFailureConverter  implements PayloadConverter<UpscaleFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return ValidateNewInstancesHealthFailedEvent.class.isAssignableFrom(sourceClass);
    }

    @Override
    public UpscaleFailureEvent convert(Object payload) {
        ValidateNewInstancesHealthFailedEvent failedEvent = (ValidateNewInstancesHealthFailedEvent) payload;
        return new UpscaleFailureEvent(failedEvent.getResourceId(), failedEvent.getFailedPhase(), failedEvent.getSuccess(),
                failedEvent.getFailureDetails(), failedEvent.getException());
    }

}