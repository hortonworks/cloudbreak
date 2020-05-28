package com.sequenceiq.environment.parameters.dao.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;

import javax.persistence.AttributeConverter;

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