package com.sequenceiq.cloudbreak.domain.converter;

import javax.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;

public class StructuredEventTypeConverterTest extends DefaultEnumConverterBaseTest<StructuredEventType> {

    @Override
    public StructuredEventType getDefaultValue() {
        return StructuredEventType.NOTIFICATION;
    }

    @Override
    public AttributeConverter<StructuredEventType, String> getVictim() {
        return new StructuredEventTypeConverter();
    }
}