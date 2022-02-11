package com.sequenceiq.cloudbreak.domain.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;

@Component
public class StackPatchTypeConverter extends DefaultEnumConverter<StackPatchType>  {

    @Override
    public StackPatchType getDefault() {
        return StackPatchType.UNKNOWN;
    }
}
