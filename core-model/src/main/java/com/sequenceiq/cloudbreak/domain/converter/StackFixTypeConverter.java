package com.sequenceiq.cloudbreak.domain.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.cloudbreak.domain.stack.StackFix.StackFixType;

public class StackFixTypeConverter extends DefaultEnumConverter<StackFixType>  {

    @Override
    public StackFixType getDefault() {
        return StackFixType.UNKNOWN;
    }
}
