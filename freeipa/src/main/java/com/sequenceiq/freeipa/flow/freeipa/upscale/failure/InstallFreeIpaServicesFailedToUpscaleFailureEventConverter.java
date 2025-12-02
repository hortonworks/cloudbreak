package com.sequenceiq.freeipa.flow.freeipa.upscale.failure;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

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
        return new UpscaleFailureEvent(
                result.getResourceId(),
                "Installing FreeIPA services",
                Set.of(),
                ERROR,
                Map.of(),
                result.getException()
        );
    }
}