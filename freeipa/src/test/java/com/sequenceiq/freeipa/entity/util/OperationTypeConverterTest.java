package com.sequenceiq.freeipa.entity.util;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;

import javax.persistence.AttributeConverter;

public class OperationTypeConverterTest extends DefaultEnumConverterBaseTest<OperationType> {

    @Override
    public OperationType getDefaultValue() {
        return OperationType.CLEANUP;
    }

    @Override
    public AttributeConverter<OperationType, String> getVictim() {
        return new OperationTypeConverter();
    }
}