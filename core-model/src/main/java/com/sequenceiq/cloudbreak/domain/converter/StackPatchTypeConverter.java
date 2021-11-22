package com.sequenceiq.cloudbreak.domain.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;

public class StackPatchTypeConverter extends DefaultEnumConverter<StackPatchType>  {

    @Override
    public StackPatchType getDefault() {
        return StackPatchType.UNKNOWN;
    }
}
