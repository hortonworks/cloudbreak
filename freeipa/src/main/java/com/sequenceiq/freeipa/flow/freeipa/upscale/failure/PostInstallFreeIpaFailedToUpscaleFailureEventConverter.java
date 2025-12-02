package com.sequenceiq.freeipa.flow.freeipa.upscale.failure;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

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
        return new UpscaleFailureEvent(
                result.getResourceId(),
                "Post installing FreeIPA",
                Set.of(),
                ERROR,
                Map.of(),
                result.getException()
        );
    }
}