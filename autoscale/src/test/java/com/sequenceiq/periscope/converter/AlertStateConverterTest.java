package com.sequenceiq.periscope.converter;

import javax.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.periscope.api.model.AlertState;

public class AlertStateConverterTest extends DefaultEnumConverterBaseTest<AlertState> {

    @Override
    public AlertState getDefaultValue() {
        return AlertState.OK;
    }

    @Override
    public AttributeConverter<AlertState, String> getVictim() {
        return new AlertStateConverter();
    }
}