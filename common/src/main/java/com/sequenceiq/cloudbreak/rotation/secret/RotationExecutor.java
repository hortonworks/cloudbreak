package com.sequenceiq.cloudbreak.rotation.secret;

import com.sequenceiq.cloudbreak.rotation.secret.step.SecretRotationStep;

public interface RotationExecutor<C extends RotationContext> {

    void rotate(C rotationContext);

    void rollback(C rotationContext);

    void finalize(C rotationContext);

    SecretRotationStep getType();

    Class<C> getContextClass();

    default C castContext(RotationContext context) {
        if (context.getClass().isAssignableFrom(getContextClass())) {
            return (C) context;
        }
        throw new SecretRotationException(String.format("Type of provided context for rotation step %s is not correct.", getType()), getType());
    }

    default void executeRotate(RotationContext context) {
        rotate(castContext(context));
    }

    default void executeRollback(RotationContext context) {
        rollback(castContext(context));
    }

    default void executeFinalize(RotationContext context) {
        finalize(castContext(context));
    }
}
