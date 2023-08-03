package com.sequenceiq.freeipa.service.rotation;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;

import java.util.List;

import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;

public enum TestFreeipaSecretType implements SecretType {
    IPA_SECRET_1,
    IPA_SECRET_2,
    IPA_SECRET_3;

    @Override
    public List<SecretRotationStep> getSteps() {
        return List.of(CUSTOM_JOB);
    }

    @Override
    public boolean multiSecret() {
        return true;
    }

    @Override
    public Class<? extends Enum<?>> getClazz() {
        return TestFreeipaSecretType.class;
    }

    @Override
    public String value() {
        return name();
    }
}
