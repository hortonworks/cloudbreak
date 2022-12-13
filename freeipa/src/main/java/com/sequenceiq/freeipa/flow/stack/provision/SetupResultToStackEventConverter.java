package com.sequenceiq.freeipa.flow.stack.provision;

import com.sequenceiq.cloudbreak.cloud.event.setup.SetupResult;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class SetupResultToStackEventConverter implements PayloadConverter<StackEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return SetupResult.class.isAssignableFrom(sourceClass);
    }

    @Override
    public StackEvent convert(Object payload) {
        return new StackEvent(((Payload) payload).getResourceId());
    }
}
