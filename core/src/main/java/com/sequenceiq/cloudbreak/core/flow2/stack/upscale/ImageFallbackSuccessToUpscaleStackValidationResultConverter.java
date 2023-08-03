package com.sequenceiq.cloudbreak.core.flow2.stack.upscale;

import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackValidationResult;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ImageFallbackSuccess;
import com.sequenceiq.flow.core.PayloadConverter;

public class ImageFallbackSuccessToUpscaleStackValidationResultConverter implements PayloadConverter<UpscaleStackValidationResult> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return ImageFallbackSuccess.class.isAssignableFrom(sourceClass);
    }

    @Override
    public UpscaleStackValidationResult convert(Object payload) {
        return new UpscaleStackValidationResult(((Payload) payload).getResourceId());
    }
}
