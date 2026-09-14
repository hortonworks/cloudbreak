package com.sequenceiq.freeipa.entity.util;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.freeipa.entity.StackPatchStatus;

public class StackPatchStatusConverter extends DefaultEnumConverter<StackPatchStatus> {

    @Override
    public StackPatchStatus getDefault() {
        return StackPatchStatus.UNKNOWN;
    }
}
