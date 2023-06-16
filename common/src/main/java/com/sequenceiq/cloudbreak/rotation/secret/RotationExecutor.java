package com.sequenceiq.cloudbreak.rotation.secret;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.rotation.secret.step.SecretRotationStep;

public interface RotationExecutor<C extends RotationContext> {

    Logger LOGGER = LoggerFactory.getLogger(RotationExecutor.class);

    void rotate(C rotationContext) throws Exception;

    void rollback(C rotationContext) throws Exception;

    void finalize(C rotationContext) throws Exception;

    void preValidate(C rotationContext) throws Exception;

    void postValidate(C rotationContext) throws Exception;

    SecretRotationStep getType();

    Class<C> getContextClass();

    default C castContext(RotationContext context) {
        if (getContextClass().isAssignableFrom(context.getClass())) {
            return (C) context;
        }
        throw new SecretRotationException(String.format("Type of provided context for rotation step %s is not correct.", getType()), getType());
    }

    default void executeRotate(RotationContext context) {
        try {
            rotate(castContext(context));
        } catch (Exception e) {
            String errorMessage = String.format("Rotation failed at %s step for %s", getType(), context.getResourceCrn());
            logAndThrow(e, errorMessage);
        }
    }

    default void executeRollback(RotationContext context) {
        try {
            rollback(castContext(context));
        } catch (Exception e) {
            String errorMessage = String.format("Rollback of rotation failed at %s step for %s", getType(), context.getResourceCrn());
            logAndThrow(e, errorMessage);
        }
    }

    default void executeFinalize(RotationContext context) {
        try {
            finalize(castContext(context));
        } catch (Exception e) {
            String errorMessage = String.format("Finalize rotation failed at %s step for %s", getType(), context.getResourceCrn());
            logAndThrow(e, errorMessage);
        }
    }

    default void executePreValidation(RotationContext context) {
        try {
            preValidate(castContext(context));
        } catch (Exception e) {
            String errorMessage = String.format("Pre validation of rotation failed at %s step for %s", getType(), context.getResourceCrn());
            logAndThrow(e, errorMessage);
        }
    }

    default void executePostValidation(RotationContext context) {
        try {
            postValidate(castContext(context));
        } catch (Exception e) {
            String errorMessage = String.format("Post validation of rotation failed at %s step for %s", getType(), context.getResourceCrn());
            logAndThrow(e, errorMessage);
        }
    }

    private void logAndThrow(Exception e, String errorMessage) {
        LOGGER.error(errorMessage, e);
        throw new SecretRotationException(errorMessage, e, getType());
    }
}
