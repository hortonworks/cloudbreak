package com.sequenceiq.cloudbreak.rotation.executor;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.service.RotationMetadata;
import com.sequenceiq.cloudbreak.util.CheckedConsumer;

public abstract class AbstractRotationExecutor<C extends RotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRotationExecutor.class);

    protected abstract void rotate(C rotationContext) throws Exception;

    protected abstract void rollback(C rotationContext) throws Exception;

    protected abstract void finalizeRotation(C rotationContext) throws Exception;

    protected abstract void preValidate(C rotationContext) throws Exception;

    protected abstract void postValidate(C rotationContext) throws Exception;

    protected abstract Class<C> getContextClass();

    public abstract SecretRotationStep getType();

    public final void executeRotate(RotationContext context, RotationMetadata metadata) {
        invokeRotationPhase(context, metadata, this::rotate,
                () -> String.format("Execution of rotation failed at %s step regarding secret %s", getType(), metadata.secretType()));
    }

    public final void executeRollback(RotationContext context, RotationMetadata metadata) {
        invokeRotationPhase(context, metadata, this::rollback,
                () -> String.format("Rollback of rotation failed at %s step regarding secret %s", getType(), metadata.secretType()));
    }

    public final void executeFinalize(RotationContext context, RotationMetadata metadata) {
        invokeRotationPhase(context, metadata, this::finalizeRotation,
                () -> String.format("Finalization of rotation failed at %s step regarding secret %s", getType(), metadata.secretType()));
    }

    public final void executePreValidation(RotationContext context, RotationMetadata metadata) {
        invokeRotationPhase(context, metadata, this::preValidate,
                () -> String.format("Pre validation of rotation failed at %s step", getType()));
    }

    public final void executePostValidation(RotationContext context, RotationMetadata metadata) {
        invokeRotationPhaseWithoutNotification(context, metadata, this::postValidate,
                () -> String.format("Post validation of rotation failed at %s step", getType()));
    }

    private void logAndThrow(Exception e, String errorMessage) {
        LOGGER.error(errorMessage, e);
        throw new SecretRotationException(String.format("%s, reason: %s", errorMessage, e.getMessage()), e);
    }

    private void invokeRotationPhase(RotationContext context, RotationMetadata metadata,
            CheckedConsumer<C, Exception> rotationPhaseLogic, Supplier<String> errorMessageSupplier) {
        try {
            rotationPhaseLogic.accept(castContext(context));
        } catch (Exception e) {
            logAndThrow(e, errorMessageSupplier.get());
        }
    }

    private void invokeRotationPhaseWithoutNotification(RotationContext context, RotationMetadata metadata,
            CheckedConsumer<C, Exception> rotationPhaseLogic, Supplier<String> errorMessageSupplier) {
        try {
            rotationPhaseLogic.accept(castContext(context));
        } catch (Exception e) {
            logAndThrow(e, errorMessageSupplier.get());
        }
    }

    private C castContext(RotationContext context) {
        if (getContextClass().isAssignableFrom(context.getClass())) {
            return (C) context;
        }
        throw new SecretRotationException(String.format("Type of provided context for rotation step %s is not correct.", getType()));
    }
}
