package com.sequenceiq.cloudbreak.structuredevent.domain.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;

public class StructuredEventTypeConverter extends DefaultEnumConverter<StructuredEventType> {

    @Override
    public StructuredEventType getDefault() {
        return StructuredEventType.NOTIFICATION;
    }
}
