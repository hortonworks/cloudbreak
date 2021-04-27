package com.sequenceiq.freeipa.flow.freeipa.upscale.failure;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.postinstall.PostInstallFreeIpaFailed;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleFailureEvent;

public class PostInstallFreeIpaFailedToUpscaleFailureEventConverter implements PayloadConverter<UpscaleFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return PostInstallFreeIpaFailed.class.isAssignableFrom(sourceClass);
    }

    @Override
    public UpscaleFailureEvent convert(Object payload) {
        PostInstallFreeIpaFailed result = (PostInstallFreeIpaFailed) payload;
        UpscaleFailureEvent event = new UpscaleFailureEvent(result.getResourceId(), "Post installing FreeIPA", Set.of(), Map.of(),
                new Exception("Payload failed: " + payload));
        return event;
    }
}
