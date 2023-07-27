package com.sequenceiq.cloudbreak.rotation;

public interface SecretRotationStep extends SerializableRotationEnum {

    default boolean skipNotification() {
        return false;
    }
}
