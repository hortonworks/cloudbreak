package com.sequenceiq.cloudbreak.rotation.common;

import static com.sequenceiq.cloudbreak.rotation.SecretTypeFlag.INTERNAL;
import static com.sequenceiq.cloudbreak.rotation.SecretTypeFlag.MULTI_SECRET;
import static com.sequenceiq.cloudbreak.rotation.SecretTypeFlag.SKIP_SALT_UPDATE;
import static com.sequenceiq.cloudbreak.rotation.common.TestSecretRotationStep.STEP;

import java.util.List;
import java.util.Set;

import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.SecretTypeFlag;

public enum TestSecretType implements SecretType {
    TEST(Set.of(SKIP_SALT_UPDATE)),
    TEST_2(Set.of(MULTI_SECRET)),
    TEST_3(Set.of(INTERNAL)),
    TEST_4(Set.of(MULTI_SECRET));

    private final Set<SecretTypeFlag> flags;

    TestSecretType(Set<SecretTypeFlag> flags) {
        this.flags = flags;
    }

    @Override
    public List<SecretRotationStep> getSteps() {
        return List.of(STEP);
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
