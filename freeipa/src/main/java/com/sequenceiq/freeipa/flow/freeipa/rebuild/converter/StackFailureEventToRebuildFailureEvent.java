package com.sequenceiq.freeipa.flow.freeipa.rebuild.converter;

import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.RebuildFailureEvent;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

public class StackFailureEventToRebuildFailureEvent implements PayloadConverter<RebuildFailureEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return StackFailureEvent.class.isAssignableFrom(sourceClass);
    }

    @Override
    public RebuildFailureEvent convert(Object payload) {
        StackFailureEvent result = (StackFailureEvent) payload;
        return new RebuildFailureEvent(result.getResourceId(), result.getException());
    }
}
