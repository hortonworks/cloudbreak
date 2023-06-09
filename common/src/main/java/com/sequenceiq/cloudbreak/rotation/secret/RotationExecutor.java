package com.sequenceiq.cloudbreak.rotation.secret;

import com.sequenceiq.cloudbreak.rotation.secret.step.SecretRotationStep;

public interface RotationExecutor<C extends RotationContext> {

    void rotate(C rotationContext) throws Exception;

    void rollback(C rotationContext) throws Exception;

    void finalize(C rotationContext) throws Exception;

    void preValidate(C rotationContext) throws Exception;

    void postValidate(C rotationContext) throws Exception;

    SecretRotationStep getType();

    Class<C> getContextClass();
}
