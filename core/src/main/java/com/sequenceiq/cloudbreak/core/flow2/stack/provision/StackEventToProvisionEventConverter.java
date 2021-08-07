package com.sequenceiq.cloudbreak.core.flow2.stack.provision;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ProvisionEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ProvisionType;
import com.sequenceiq.flow.core.PayloadConverter;

// TODO: this is there for backward compatibility
//  should be removed if it is guaranteed that
//  there is no running provision flow using it
//  Tracked here: CB-13743
public class StackEventToProvisionEventConverter implements PayloadConverter<ProvisionEvent> {

    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return StackEvent.class.isAssignableFrom(sourceClass);
    }

    @Override
    public ProvisionEvent convert(Object payload) {
        StackEvent stackEvent = (StackEvent) payload;
        return new ProvisionEvent(
                stackEvent.selector(),
                stackEvent.getResourceId(),
                ProvisionType.REGULAR,
                stackEvent.accepted());
    }
}
