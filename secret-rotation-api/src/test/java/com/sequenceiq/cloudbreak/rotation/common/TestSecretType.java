package com.sequenceiq.cloudbreak.rotation.common;

import static com.sequenceiq.cloudbreak.rotation.SecretTypeFlag.INTERNAL;
import static com.sequenceiq.cloudbreak.rotation.SecretTypeFlag.SKIP_SALT_UPDATE;
import static com.sequenceiq.cloudbreak.rotation.SecretTypeFlag.SKIP_STATUS_CHECK;
import static com.sequenceiq.cloudbreak.rotation.common.TestSecretRotationStep.STEP;
import static com.sequenceiq.cloudbreak.rotation.common.TestSecretRotationStep.STEP2;
import static com.sequenceiq.cloudbreak.rotation.common.TestSecretRotationStep.STEP3;

import java.util.List;
import java.util.Set;

import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.SecretTypeFlag;

public enum TestSecretType implements SecretType {
    TEST(Set.of(SKIP_SALT_UPDATE)),
    TEST_2,
    TEST_3(Set.of(INTERNAL)),
    TEST_4(Set.of(SKIP_STATUS_CHECK));

    private final Set<SecretTypeFlag> flags;

    TestSecretType(Set<SecretTypeFlag> flags) {
        this.flags = flags;
    }

    TestSecretType() {
        this.flags = Set.of();
    }

    @Override
    public List<SecretRotationStep> getSteps() {
        return List.of(STEP, STEP2, STEP3);
    }

    @Override
    public Set<SecretTypeFlag> getFlags() {
        return flags;
    }

    @Override
    public Class<? extends Enum<?>> getClazz() {
        return TestSecretType.class;
    }

    @Override
    public String value() {
        return name();
    }
}
