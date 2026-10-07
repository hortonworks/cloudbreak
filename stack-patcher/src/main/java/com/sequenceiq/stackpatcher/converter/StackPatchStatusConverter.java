package com.sequenceiq.stackpatcher.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.stackpatcher.domain.StackPatchStatus;

@Component
public class StackPatchStatusConverter extends DefaultEnumConverter<StackPatchStatus> {

    @Override
    public StackPatchStatus getDefault() {
        return StackPatchStatus.UNKNOWN;
    }
}
