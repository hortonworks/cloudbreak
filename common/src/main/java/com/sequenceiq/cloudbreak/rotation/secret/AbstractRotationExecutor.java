package com.sequenceiq.cloudbreak.rotation.secret;

import java.util.Optional;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.util.CheckedConsumer;

public abstract class AbstractRotationExecutor<C extends RotationContext> implements RotationExecutor<C> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRotationExecutor.class);

    @Inject
    private Optional<SecretRotationProgressService> secretRotationProgressService;

    public final void executeRotate(RotationContext context, SecretType secretType) {
        invokeRotationPhaseWithProgressCheck(context, secretType, RotationFlowExecutionType.ROTATE, this::rotate,
                () -> String.format("Execution of rotation failed at %s step for %s regarding secret %s.", getType(), context.getResourceCrn(), secretType));
    }

    public final void executeRollback(RotationContext context, SecretType secretType) {
        invokeRotationPhaseWithProgressCheck(context, secretType, RotationFlowExecutionType.ROLLBACK, this::rollback,
                () -> String.format("Rollback of rotation failed at %s step for %s regarding secret %s.", getType(), context.getResourceCrn(), secretType));
    }

    public final void executeFinalize(RotationContext context, SecretType secretType) {
        invokeRotationPhaseWithProgressCheck(context, secretType, RotationFlowExecutionType.FINALIZE, this::finalize,
                () -> String.format("Finalization of rotation failed at %s step for %s regarding secret %s.", getType(), context.getResourceCrn(), secretType));
    }

    public final void executePreValidation(RotationContext context) {
        invokeRotationPhase(context, this::preValidate,
                () -> String.format("Pre validation of rotation failed at %s step for %s", getType(), context.getResourceCrn()));
    }

    public final void executePostValidation(RotationContext context) {
        invokeRotationPhase(context, this::postValidate,
                () -> String.format("Post validation of rotation failed at %s step for %s", getType(), context.getResourceCrn()));
    }

    private void logAndThrow(Exception e, String errorMessage) {
        LOGGER.error(errorMessage, e);
        throw new SecretRotationException(errorMessage, e, getType());
    }

    private void invokeRotationPhaseWithProgressCheck(RotationContext context, SecretType secretType, RotationFlowExecutionType executionType,
            CheckedConsumer<C, Exception> rotationPhaseLogic, Supplier<String> errorMessageSupplier) {
        secretRotationProgressService.ifPresentOrElse(progressService -> {
            Optional latestStepProgress = progressService.latestStep(context.getResourceCrn(), secretType, getType(), executionType);
            if (latestStepProgress.isEmpty() || !progressService.isFinished(latestStepProgress.get())) {
                try {
                    rotationPhaseLogic.accept(castContext(context));
                    latestStepProgress.ifPresent(progressService::finished);
                } catch (Exception e) {
                    latestStepProgress.ifPresent(progressService::finished);
                    logAndThrow(e, errorMessageSupplier.get());
                }
            } else {
                LOGGER.info("{} is already finished for {} step regarding {} secret, thus skipping it.", executionType, getType(), secretType);
            }
        }, () -> invokeRotationPhase(context, rotationPhaseLogic, errorMessageSupplier));
    }

    private void invokeRotationPhase(RotationContext context, CheckedConsumer<C, Exception> rotationPhaseLogic, Supplier<String> errorMessageSupplier) {
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
        throw new SecretRotationException(String.format("Type of provided context for rotation step %s is not correct.", getType()), getType());
    }
}
