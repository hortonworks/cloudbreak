package com.sequenceiq.cloudbreak.rotation.secret;

public interface RotationExecutor<C extends RotationContext> {

    void rotate(C rotationContext);

    void rollback(C rotationContext);

    void finalize(C rotationContext);

    SecretRotationStep getType();
}
