package com.sequenceiq.freeipa.entity.util;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;

public class OperationTypeConverter extends DefaultEnumConverter<OperationType> {

    @Override
    public OperationType getDefault() {
        return OperationType.CLEANUP;
    }
}
