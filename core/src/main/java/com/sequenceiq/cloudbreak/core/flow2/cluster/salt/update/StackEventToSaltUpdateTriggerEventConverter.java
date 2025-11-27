package com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.SaltUpdateTriggerEvent;
import com.sequenceiq.flow.core.PayloadConverter;

public class StackEventToSaltUpdateTriggerEventConverter implements PayloadConverter<SaltUpdateTriggerEvent> {

    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return StackEvent.class.isAssignableFrom(sourceClass);
    }

    @Override
    public SaltUpdateTriggerEvent convert(Object payload) {
        return new SaltUpdateTriggerEvent(((StackEvent) payload).getResourceId());
    }
}
