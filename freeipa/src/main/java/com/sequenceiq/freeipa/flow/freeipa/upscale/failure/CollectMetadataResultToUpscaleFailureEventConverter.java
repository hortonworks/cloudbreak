package com.sequenceiq.freeipa.flow.freeipa.upscale.failure;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleFailureEvent;

public class CollectMetadataResultToUpscaleFailureEventConverter implements PayloadConverter<UpscaleFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return CollectMetadataResult.class.isAssignableFrom(sourceClass);
    }

    @Override
    public UpscaleFailureEvent convert(Object payload) {
        CollectMetadataResult result = (CollectMetadataResult) payload;
        UpscaleFailureEvent event = new UpscaleFailureEvent(result.getResourceId(), "Collecting metadata", Set.of(), Map.of(),
                new Exception("Payload failed: " + payload));
        return event;
    }
}
