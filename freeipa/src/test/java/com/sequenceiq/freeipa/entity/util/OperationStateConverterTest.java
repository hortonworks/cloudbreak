package com.sequenceiq.freeipa.entity.util;

import jakarta.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;

public class OperationStateConverterTest extends DefaultEnumConverterBaseTest<OperationState> {

    @Override
    public OperationState getDefaultValue() {
        return OperationState.RUNNING;
    }

    @Override
    public AttributeConverter<OperationState, String> getVictim() {
        return new OperationStateConverter();
    }
}
