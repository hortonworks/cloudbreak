package com.sequenceiq.cloudbreak.rotation.entity;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;

public class SecretRotationStepProgressStatusConverter extends DefaultEnumConverter<SecretRotationStepProgressStatus> {

    @Override
    public SecretRotationStepProgressStatus getDefault() {
        return SecretRotationStepProgressStatus.IN_PROGRESS;
    }
}
