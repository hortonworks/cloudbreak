package com.sequenceiq.cloudbreak.core.flow2.stack.upscale;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.UpscaleStackImageFallbackResult;
import com.sequenceiq.flow.core.PayloadConverter;

public class UpscaleStackImageFallbackResultToStackEventConverter implements PayloadConverter<StackEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return UpscaleStackImageFallbackResult.class.isAssignableFrom(sourceClass);
    }

    @Override
    public StackEvent convert(Object payload) {
        return new StackEvent(((Payload) payload).getResourceId());
    }
}
