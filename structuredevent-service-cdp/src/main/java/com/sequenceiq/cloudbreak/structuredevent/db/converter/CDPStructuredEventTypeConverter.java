package com.sequenceiq.cloudbreak.structuredevent.db.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;

public class CDPStructuredEventTypeConverter extends DefaultEnumConverter<StructuredEventType> {

    @Override
    public StructuredEventType getDefault() {
        return StructuredEventType.NOTIFICATION;
    }
}
