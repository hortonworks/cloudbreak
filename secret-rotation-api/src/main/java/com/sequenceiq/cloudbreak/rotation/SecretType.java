package com.sequenceiq.cloudbreak.rotation;

import java.util.List;

public interface SecretType extends SerializableRotationEnum {

    List<SecretRotationStep> getSteps();

    default boolean internal() {
        return false;
    }

    default boolean multiSecret() {
        return false;
    }
}
