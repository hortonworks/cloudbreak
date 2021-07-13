package com.sequenceiq.flow.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.flow.api.model.operation.OperationType;

public class OperationTypeConverter extends DefaultEnumConverter<OperationType> {

    @Override
    public OperationType getDefault() {
        return OperationType.UNKNOWN;
    }

    @Override
    public OperationType convertToEntityAttribute(String attribute) {
        if (attribute == null) {
            attribute = OperationType.UNKNOWN.name();
        }
        return super.convertToEntityAttribute(attribute);
    }
}
