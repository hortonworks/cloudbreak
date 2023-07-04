package com.sequenceiq.cloudbreak.rotation.entity;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;

public class RotationFlowExecutionTypeConverter extends DefaultEnumConverter<RotationFlowExecutionType> {

    @Override
    public RotationFlowExecutionType getDefault() {
        return RotationFlowExecutionType.ROTATE;
    }
}
