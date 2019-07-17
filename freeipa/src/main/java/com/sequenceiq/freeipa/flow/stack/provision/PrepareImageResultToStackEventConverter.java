package com.sequenceiq.freeipa.flow.stack.provision;

import com.sequenceiq.cloudbreak.cloud.event.setup.PrepareImageResult;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class PrepareImageResultToStackEventConverter implements PayloadConverter<StackEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return PrepareImageResult.class.isAssignableFrom(sourceClass);
    }

    @Override
    public StackEvent convert(Object payload) {
        return new StackEvent(((Payload) payload).getResourceId());
    }
}
