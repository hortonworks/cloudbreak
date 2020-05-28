package com.sequenceiq.environment.parameters.dao.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.environment.environment.EnvironmentStatus;

import javax.persistence.AttributeConverter;

public class EnvironmentStatusConverterTest extends DefaultEnumConverterBaseTest<EnvironmentStatus> {

    @Override
    public EnvironmentStatus getDefaultValue() {
        return EnvironmentStatus.AVAILABLE;
    }

    @Override
    public AttributeConverter<EnvironmentStatus, String> getVictim() {
        return new EnvironmentStatusConverter();
    }
}