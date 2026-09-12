package com.sequenceiq.freeipa.entity.util;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.freeipa.entity.StackPatchType;

public class StackPatchTypeConverter extends DefaultEnumConverter<StackPatchType> {

    @Override
    public StackPatchType getDefault() {
        return StackPatchType.UNKNOWN;
    }
}
