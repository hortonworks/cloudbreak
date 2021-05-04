package com.sequenceiq.freeipa.flow.freeipa.downscale.failure;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackResult;
import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleFailureEvent;

public class DownscaleStackResultToDownscaleFailureEventConverter implements PayloadConverter<DownscaleFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return DownscaleStackResult.class.isAssignableFrom(sourceClass);
    }

    @Override
    public DownscaleFailureEvent convert(Object payload) {
        DownscaleStackResult downscaleStackResult = (DownscaleStackResult) payload;
        DownscaleFailureEvent event = new DownscaleFailureEvent(downscaleStackResult.getResourceId(),  "Downscale Stack",
                Set.of(), Map.of(), new Exception("Payload failed: " + payload));
        return event;
    }
}
