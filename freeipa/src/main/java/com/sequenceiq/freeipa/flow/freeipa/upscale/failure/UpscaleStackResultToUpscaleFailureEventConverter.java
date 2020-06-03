package com.sequenceiq.freeipa.flow.freeipa.upscale.failure;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackResult;
import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleFailureEvent;

public class UpscaleStackResultToUpscaleFailureEventConverter implements PayloadConverter<UpscaleFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return UpscaleStackResult.class.isAssignableFrom(sourceClass);
    }

    @Override
    public UpscaleFailureEvent convert(Object payload) {
        UpscaleStackResult result = (UpscaleStackResult) payload;
        UpscaleFailureEvent event = new UpscaleFailureEvent(result.getResourceId(), "Adding instances", Set.of(), Map.of(),
                new Exception("Payload failed: " + payload));
        return event;
    }
}
