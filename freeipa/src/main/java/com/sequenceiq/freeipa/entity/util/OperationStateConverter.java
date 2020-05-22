package com.sequenceiq.freeipa.entity.util;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;

public class OperationStateConverter extends DefaultEnumConverter<OperationState> {

    @Override
    public OperationState getDefault() {
        return OperationState.RUNNING;
    }
}
