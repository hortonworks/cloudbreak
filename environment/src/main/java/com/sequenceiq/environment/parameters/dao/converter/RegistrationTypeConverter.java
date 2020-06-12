package com.sequenceiq.environment.parameters.dao.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;

public class RegistrationTypeConverter extends DefaultEnumConverter<RegistrationType> {

    @Override
    public RegistrationType getDefault() {
        return RegistrationType.EXISTING;
    }
}
