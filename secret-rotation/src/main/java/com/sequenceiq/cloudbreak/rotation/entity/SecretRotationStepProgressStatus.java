package com.sequenceiq.cloudbreak.rotation.entity;

import com.sequenceiq.cloudbreak.rotation.SerializableRotationEnum;

public enum SecretRotationStepProgressStatus implements SerializableRotationEnum {
    IN_PROGRESS,
    FINISHED,
    FAILED;

    @Override
    public Class<? extends Enum<?>> getClazz() {
        return SecretRotationStepProgressStatus.class;
    }

    @Override
    public String value() {
        return name();
    }
}
