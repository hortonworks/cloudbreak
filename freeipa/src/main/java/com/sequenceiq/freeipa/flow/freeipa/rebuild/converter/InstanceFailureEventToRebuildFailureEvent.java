package com.sequenceiq.freeipa.flow.freeipa.rebuild.converter;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.RebuildFailureEvent;
import com.sequenceiq.freeipa.flow.instance.InstanceFailureEvent;

public class InstanceFailureEventToRebuildFailureEvent implements PayloadConverter<RebuildFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return InstanceFailureEvent.class.isAssignableFrom(sourceClass);
    }

    @Override
    public RebuildFailureEvent convert(Object payload) {
        InstanceFailureEvent result = (InstanceFailureEvent) payload;
        return new RebuildFailureEvent(result.getResourceId(), ERROR, result.getException());
    }
}
