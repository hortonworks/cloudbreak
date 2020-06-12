package com.sequenceiq.freeipa.entity.util;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;

import javax.persistence.AttributeConverter;

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