package com.sequenceiq.cloudbreak.domain.converter;

import jakarta.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ExecutorType;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;

public class ExecutorTypeConverterTest extends DefaultEnumConverterBaseTest<ExecutorType> {

    @Override
    public ExecutorType getDefaultValue() {
        return ExecutorType.DEFAULT;
    }

    @Override
    public AttributeConverter<ExecutorType, String> getVictim() {
        return new ExecutorTypeConverter();
    }
}
