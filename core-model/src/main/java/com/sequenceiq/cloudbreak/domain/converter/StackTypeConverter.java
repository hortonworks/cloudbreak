package com.sequenceiq.cloudbreak.domain.converter;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;

public class StackTypeConverter extends DefaultEnumConverter<StackType> {

    @Override
    public StackType getDefault() {
        return StackType.DATALAKE;
    }
}
