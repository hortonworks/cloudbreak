package com.sequenceiq.stackpatcher.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.stackpatcher.domain.StackPatchType;

@Component
public class StackPatchTypeConverter extends DefaultEnumConverter<StackPatchType> {

    @Override
    public StackPatchType getDefault() {
        return StackPatchType.UNKNOWN;
    }
}
