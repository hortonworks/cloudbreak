package com.sequenceiq.freeipa.flow.freeipa.downscale.failure;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.removeserver.RemoveServersResponse;

public class RemoveServersResponseToDownscaleFailureEventConverter implements PayloadConverter<DownscaleFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return RemoveServersResponse.class.isAssignableFrom(sourceClass);
    }

    @Override
    public DownscaleFailureEvent convert(Object payload) {
        RemoveServersResponse removeServersResponse = (RemoveServersResponse) payload;
        DownscaleFailureEvent event = new DownscaleFailureEvent(removeServersResponse.getResourceId(),  "Server removal",
                Set.of(), Map.of(), new Exception("Payload failed: " + payload));
        return event;
    }
}
