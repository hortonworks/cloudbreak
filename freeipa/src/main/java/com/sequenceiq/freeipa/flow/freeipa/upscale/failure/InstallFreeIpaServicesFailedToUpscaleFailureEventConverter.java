package com.sequenceiq.freeipa.flow.freeipa.upscale.failure;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.services.InstallFreeIpaServicesFailed;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleFailureEvent;

public class InstallFreeIpaServicesFailedToUpscaleFailureEventConverter implements PayloadConverter<UpscaleFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return InstallFreeIpaServicesFailed.class.isAssignableFrom(sourceClass);
    }

    @Override
    public UpscaleFailureEvent convert(Object payload) {
        InstallFreeIpaServicesFailed result = (InstallFreeIpaServicesFailed) payload;
        UpscaleFailureEvent event = new UpscaleFailureEvent(result.getResourceId(), "Installing FreeIPA services", Set.of(), Map.of(),
                new Exception("Payload failed: " + payload));
        return event;
    }
}
