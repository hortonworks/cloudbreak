package com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event;

import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class StackEventToLoadBalancerCreationTriggerEventConverter implements PayloadConverter<LoadBalancerCreationTriggerEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return StackEvent.class.isAssignableFrom(sourceClass);
    }

    @Override
    public LoadBalancerCreationTriggerEvent convert(Object payload) {
        StackEvent stackEvent = (StackEvent) payload;
        return new LoadBalancerCreationTriggerEvent(stackEvent.selector(), stackEvent.getResourceId());
    }
}
