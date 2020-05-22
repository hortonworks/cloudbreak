package com.sequenceiq.cloudbreak.domain.converter;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ExecutorType;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;

import javax.persistence.AttributeConverter;

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