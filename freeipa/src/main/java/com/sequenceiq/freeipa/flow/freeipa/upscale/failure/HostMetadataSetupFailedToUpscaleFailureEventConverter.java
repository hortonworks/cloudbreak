package com.sequenceiq.freeipa.flow.freeipa.upscale.failure;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.hostmetadatasetup.HostMetadataSetupFailed;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleFailureEvent;

public class HostMetadataSetupFailedToUpscaleFailureEventConverter implements PayloadConverter<UpscaleFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return HostMetadataSetupFailed.class.isAssignableFrom(sourceClass);
    }

    @Override
    public UpscaleFailureEvent convert(Object payload) {
        HostMetadataSetupFailed result = (HostMetadataSetupFailed) payload;
        UpscaleFailureEvent event = new UpscaleFailureEvent(result.getResourceId(), "Host metadata setup", Set.of(), Map.of(),
                new Exception("Payload failed: " + payload));
        return event;
    }
}
