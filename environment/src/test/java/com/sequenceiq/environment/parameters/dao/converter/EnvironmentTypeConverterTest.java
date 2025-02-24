package com.sequenceiq.environment.parameters.dao.converter;

import jakarta.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.common.api.type.EnvironmentType;

public class EnvironmentTypeConverterTest extends DefaultEnumConverterBaseTest<EnvironmentType> {

    @Override
    public EnvironmentType getDefaultValue() {
        return EnvironmentType.PUBLIC_CLOUD;
    }

    @Override
    public AttributeConverter<EnvironmentType, String> getVictim() {
        return new EnvironmentTypeConverter();
    }
}
