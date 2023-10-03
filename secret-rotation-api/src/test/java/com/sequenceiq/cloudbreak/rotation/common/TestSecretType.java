package com.sequenceiq.cloudbreak.rotation.common;

import static com.sequenceiq.cloudbreak.rotation.SecretTypeFlag.INTERNAL;
import static com.sequenceiq.cloudbreak.rotation.SecretTypeFlag.SKIP_SALT_UPDATE;
import static com.sequenceiq.cloudbreak.rotation.common.TestSecretRotationStep.STEP;
import static com.sequenceiq.cloudbreak.rotation.common.TestSecretRotationStep.STEP2;
import static com.sequenceiq.cloudbreak.rotation.common.TestSecretRotationStep.STEP3;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.sequenceiq.cloudbreak.rotation.MultiSecretType;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.SecretTypeFlag;

public enum TestSecretType implements SecretType {
    TEST(Set.of(SKIP_SALT_UPDATE)),
    TEST_2(MultiSecretType.DEMO_MULTI_SECRET),
    TEST_3(Set.of(INTERNAL)),
    TEST_4(MultiSecretType.DEMO_MULTI_SECRET);

    private final Set<SecretTypeFlag> flags;

    private final Optional<MultiSecretType> multiSecretType;

    TestSecretType(Set<SecretTypeFlag> flags) {
        this.flags = flags;
        this.multiSecretType = Optional.empty();
    }

    TestSecretType(MultiSecretType multiSecretType) {
        this.flags = Set.of();
        this.multiSecretType = Optional.ofNullable(multiSecretType);
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
    public Optional<MultiSecretType> getMultiSecretType() {
        return multiSecretType;
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
