package com.sequenceiq.freeipa.flow.freeipa.downscale.failure;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackCollectResourcesResult;
import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleFailureEvent;

public class DownscaleStackCollectResourcesResultToDownscaleFailureEventConverter implements PayloadConverter<DownscaleFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return DownscaleStackCollectResourcesResult.class.isAssignableFrom(sourceClass);
    }

    @Override
    public DownscaleFailureEvent convert(Object payload) {
        DownscaleStackCollectResourcesResult downscaleStackCollectResourcesResult = (DownscaleStackCollectResourcesResult) payload;
        DownscaleFailureEvent event = new DownscaleFailureEvent(downscaleStackCollectResourcesResult.getResourceId(),  "Collecting resources",
                Set.of(), Map.of(), new Exception("Payload failed: " + payload));
        return event;
    }
}
