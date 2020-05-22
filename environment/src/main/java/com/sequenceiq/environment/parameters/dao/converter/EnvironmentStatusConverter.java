package com.sequenceiq.environment.parameters.dao.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.environment.environment.EnvironmentStatus;

public class EnvironmentStatusConverter extends DefaultEnumConverter<EnvironmentStatus> {

    @Override
    public EnvironmentStatus getDefault() {
        return EnvironmentStatus.AVAILABLE;
    }
}
