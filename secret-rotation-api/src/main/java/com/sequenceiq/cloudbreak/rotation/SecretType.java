package com.sequenceiq.cloudbreak.rotation;

import java.util.List;

public interface SecretType extends SerializableRotationEnum {
    List<SecretRotationStep> getSteps();

    default boolean multiCluster() {
        return false;
    }
}
