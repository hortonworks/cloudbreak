package com.sequenceiq.cloudbreak.domain.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchStatus;

@Component
public class StackPatchStatusConverter extends DefaultEnumConverter<StackPatchStatus>  {

    @Override
    public StackPatchStatus getDefault() {
        return StackPatchStatus.UNKNOWN;
    }
}
