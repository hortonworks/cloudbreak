package com.sequenceiq.cloudbreak.rotation;

public enum RotationFlowExecutionType implements SerializableRotationEnum {
    PREVALIDATE,
    ROLLBACK,
    FINALIZE,
    ROTATE;

    @Override
    public Class<? extends Enum<?>> getClazz() {
        return RotationFlowExecutionType.class;
    }

    @Override
    public String value() {
        return name();
    }
}
