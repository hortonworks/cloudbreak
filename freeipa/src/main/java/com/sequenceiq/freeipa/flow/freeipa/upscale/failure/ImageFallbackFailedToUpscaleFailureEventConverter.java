package com.sequenceiq.freeipa.flow.freeipa.upscale.failure;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import java.util.Map;
import java.util.Set;

import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleFailureEvent;
import com.sequenceiq.freeipa.flow.stack.provision.event.imagefallback.ImageFallbackFailed;

public class ImageFallbackFailedToUpscaleFailureEventConverter implements PayloadConverter<UpscaleFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return ImageFallbackFailed.class.isAssignableFrom(sourceClass);
    }

    @Override
    public UpscaleFailureEvent convert(Object payload) {
        ImageFallbackFailed result = (ImageFallbackFailed) payload;
        return new UpscaleFailureEvent(
                result.getResourceId(),
                "Image fallback",
                Set.of(),
                ERROR,
                result.getException() != null ? Map.of("statusReason", result.getException().getMessage()) : Map.of(),
                result.getException()
        );
    }
}
