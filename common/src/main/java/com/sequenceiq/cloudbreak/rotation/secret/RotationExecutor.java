package com.sequenceiq.cloudbreak.rotation.secret;

public interface RotationExecutor {

    void rotate(RotationContext rotationContext);

    void rollback(RotationContext rotationContext);

    void finalize(RotationContext rotationContext);

    SecretLocationType getType();
}
