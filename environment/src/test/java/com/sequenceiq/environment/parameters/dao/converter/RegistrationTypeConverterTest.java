package com.sequenceiq.environment.parameters.dao.converter;

import jakarta.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;

public class RegistrationTypeConverterTest extends DefaultEnumConverterBaseTest<RegistrationType> {

    @Override
    public RegistrationType getDefaultValue() {
        return RegistrationType.EXISTING;
    }

    @Override
    public AttributeConverter<RegistrationType, String> getVictim() {
        return new RegistrationTypeConverter();
    }
}
