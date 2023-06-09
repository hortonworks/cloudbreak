package com.sequenceiq.flow.rotation.entity;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.cloudbreak.rotation.secret.RotationFlowExecutionType;

public class RotationFlowExecutionTypeConverter extends DefaultEnumConverter<RotationFlowExecutionType> {

    @Override
    public RotationFlowExecutionType getDefault() {
        return RotationFlowExecutionType.ROTATE;
    }
}
