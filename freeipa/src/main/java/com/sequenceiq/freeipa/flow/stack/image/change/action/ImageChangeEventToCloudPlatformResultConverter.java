package com.sequenceiq.freeipa.flow.stack.image.change.action;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvent;

public class ImageChangeEventToCloudPlatformResultConverter implements PayloadConverter<CloudPlatformResult> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return ImageChangeEvent.class.isAssignableFrom(sourceClass);
    }

    @Override
    public CloudPlatformResult convert(Object payload) {
        ImageChangeEvent imageChangeEvent = (ImageChangeEvent) payload;
        return new CloudPlatformResult(imageChangeEvent.getResourceId());
    }
}
