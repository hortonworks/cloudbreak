package com.sequenceiq.periscope.converter;

import jakarta.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.periscope.api.model.AlertType;

public class AlertTypeConverterTest extends DefaultEnumConverterBaseTest<AlertType> {

    @Override
    public AlertType getDefaultValue() {
        return AlertType.METRIC;
    }

    @Override
    public AttributeConverter<AlertType, String> getVictim() {
        return new AlertTypeConverter();
    }
}
