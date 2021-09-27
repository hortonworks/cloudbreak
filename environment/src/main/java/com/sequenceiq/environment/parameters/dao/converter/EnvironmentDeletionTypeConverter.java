package com.sequenceiq.environment.parameters.dao.converter;

import static com.sequenceiq.environment.environment.EnvironmentDeletionType.NONE;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.environment.environment.EnvironmentDeletionType;

public class EnvironmentDeletionTypeConverter extends DefaultEnumConverter<EnvironmentDeletionType> {

    @Override
    public EnvironmentDeletionType getDefault() {
        return NONE;
    }
}
