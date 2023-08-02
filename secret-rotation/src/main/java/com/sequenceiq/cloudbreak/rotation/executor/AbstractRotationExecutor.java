package com.sequenceiq.cloudbreak.rotation.executor;

import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.FINALIZE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.PREVALIDATE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROLLBACK;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;

import java.util.Optional;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.entity.SecretRotationStepProgress;
import com.sequenceiq.cloudbreak.rotation.service.notification.SecretRotationNotificationService;
import com.sequenceiq.cloudbreak.rotation.service.progress.SecretRotationStepProgressService;
import com.sequenceiq.cloudbreak.util.CheckedConsumer;

public abstract class AbstractRotationExecutor<C extends RotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRotationExecutor.class);

    @Inject
    private SecretRotationStepProgressService progressService;

    @Inject
    private SecretRotationNotificationService secretRotationNotificationService;

    protected abstract void rotate(C rotationContext) throws Exception;

    protected abstract void rollback(C rotationContext) throws Exception;

    protected abstract void finalize(C rotationContext) throws Exception;

    protected abstract void preValidate(C rotationContext) throws Exception;

    protected abstract void postValidate(C rotationContext) throws Exception;

    protected abstract Class<C> getContextClass();

    public abstract SecretRotationStep getType();

    public final void executeRotate(RotationContext context, SecretType secretType) {
        invokeRotationPhaseWithProgressCheck(context, secretType, ROTATE, this::rotate,
                () -> String.format("Execution of rotation failed at %s step for %s regarding secret %s.", getType(), context.getResourceCrn(), secretType));
    }

    public final void executeRollback(RotationContext context, SecretType secretType) {
        invokeRotationPhaseWithProgressCheck(context, secretType, ROLLBACK, this::rollback,
                () -> String.format("Rollback of rotation failed at %s step for %s regarding secret %s.", getType(), context.getResourceCrn(), secretType));
    }

    public final void executeFinalize(RotationContext context, SecretType secretType) {
        invokeRotationPhaseWithProgressCheck(context, secretType, FINALIZE, this::finalize,
                () -> String.format("Finalization of rotation failed at %s step for %s regarding secret %s.", getType(), context.getResourceCrn(), secretType));
    }

    public final void executePreValidation(RotationContext context, SecretType secretType) {
        invokeRotationPhaseWithProgressCheck(context, secretType, PREVALIDATE, this::preValidate,
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
        Optional<SecretRotationStepProgress> latestStepProgress = progressService.latestStep(context.getResourceCrn(), secretType, getType(), executionType);
        if (latestStepProgress.isEmpty() || latestStepProgress.get().getFinished() == null) {
            try {
                secretRotationNotificationService.sendNotification(context.getResourceCrn(), secretType, getType(), executionType);
                rotationPhaseLogic.accept(castContext(context));
            } catch (Exception e) {
                logAndThrow(e, errorMessageSupplier.get());
            } finally {
                latestStepProgress.ifPresent(progressService::finished);
            }
        } else {
            LOGGER.info("{} is already finished for {} step regarding {} secret, thus skipping it.", executionType, getType(), secretType);
        }
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
