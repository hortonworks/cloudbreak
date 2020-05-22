package com.sequenceiq.periscope.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.periscope.api.model.AlertType;

import javax.persistence.AttributeConverter;

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