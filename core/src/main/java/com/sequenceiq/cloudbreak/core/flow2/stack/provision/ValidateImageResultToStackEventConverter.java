package com.sequenceiq.cloudbreak.core.flow2.stack.provision;

import com.sequenceiq.cloudbreak.reactor.api.event.stack.ImageUpdateEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ValidateImageResult;
import com.sequenceiq.flow.core.PayloadConverter;

public class ValidateImageResultToStackEventConverter implements PayloadConverter<ImageUpdateEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return ValidateImageResult.class.isAssignableFrom(sourceClass);
    }

    @Override
    public ImageUpdateEvent convert(Object payload) {
        ValidateImageResult validateImageResult = (ValidateImageResult) payload;
        return new ImageUpdateEvent(validateImageResult.selector(), validateImageResult.getResourceId(), validateImageResult.getImage());
    }
}
