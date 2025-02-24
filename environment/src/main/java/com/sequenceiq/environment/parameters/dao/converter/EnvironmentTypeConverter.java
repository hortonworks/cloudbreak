package com.sequenceiq.environment.parameters.dao.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.common.api.type.EnvironmentType;

public class EnvironmentTypeConverter extends DefaultEnumConverter<EnvironmentType>  {

    @Override
    public EnvironmentType getDefault() {
        return EnvironmentType.PUBLIC_CLOUD;
    }
}
