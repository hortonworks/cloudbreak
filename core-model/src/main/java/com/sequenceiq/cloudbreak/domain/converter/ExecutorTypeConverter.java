package com.sequenceiq.cloudbreak.domain.converter;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ExecutorType;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;

public class ExecutorTypeConverter extends DefaultEnumConverter<ExecutorType> {

    @Override
    public ExecutorType getDefault() {
        return ExecutorType.DEFAULT;
    }
}
