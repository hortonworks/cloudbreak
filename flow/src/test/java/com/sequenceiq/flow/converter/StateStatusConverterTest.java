package com.sequenceiq.flow.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.flow.domain.StateStatus;

import javax.persistence.AttributeConverter;

public class StateStatusConverterTest extends DefaultEnumConverterBaseTest<StateStatus> {

    @Override
    public StateStatus getDefaultValue() {
        return StateStatus.SUCCESSFUL;
    }

    @Override
    public AttributeConverter<StateStatus, String> getVictim() {
        return new StateStatusConverter();
    }
}