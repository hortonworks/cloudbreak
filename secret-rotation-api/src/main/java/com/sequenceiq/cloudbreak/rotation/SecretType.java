package com.sequenceiq.cloudbreak.rotation;

import static com.sequenceiq.cloudbreak.rotation.SecretTypeFlag.INTERNAL;
import static com.sequenceiq.cloudbreak.rotation.SecretTypeFlag.SKIP_SALT_UPDATE;
import static com.sequenceiq.cloudbreak.rotation.SecretTypeFlag.SKIP_STATUS_CHECK;

import java.util.List;
import java.util.Set;

public interface SecretType extends SerializableRotationEnum {

    List<SecretRotationStep> getSteps();

    default Set<SecretTypeFlag> getFlags() {
        return Set.of();
    }

    default boolean internal() {
        return getFlags().contains(INTERNAL);
    }

    default boolean saltUpdateNeeded() {
        return !getFlags().contains(SKIP_SALT_UPDATE);
    }

    default boolean statusCheckNeeded() {
        return !getFlags().contains(SKIP_STATUS_CHECK);
    }
}
