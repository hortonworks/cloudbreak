package com.sequenceiq.redbeams.flow.redbeams.provision.event;

import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;

public class RedbeamsEventToTriggerRedbeamsProvisionEventPayloadConverter implements PayloadConverter<TriggerRedbeamsProvisionEvent> {
    @Override
    public boolean canConvert(Class<?> sourceClass) {
        return RedbeamsEvent.class.isAssignableFrom(sourceClass);
    }

    @Override
    public TriggerRedbeamsProvisionEvent convert(Object payload) {
        RedbeamsEvent redbeamsEvent = (RedbeamsEvent) payload;
        return new TriggerRedbeamsProvisionEvent(redbeamsEvent.selector(), redbeamsEvent.getResourceId(), null);
    }
}
