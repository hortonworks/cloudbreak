package com.sequenceiq.freeipa.flow.freeipa.upscale;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.provision.event.imagefallback.ImageFallbackSuccess;

public class ImageFallbackSuccessToStackEventConverter implements PayloadConverter<StackEvent> {

    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return ImageFallbackSuccess.class.isAssignableFrom(sourceClass);
    }

    @Override
    public StackEvent convert(Object payload) {
        return new StackEvent(((Payload) payload).getResourceId());
    }
}
