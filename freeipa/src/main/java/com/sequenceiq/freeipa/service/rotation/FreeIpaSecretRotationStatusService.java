package com.sequenceiq.freeipa.service.rotation;

import static java.lang.String.format;

import jakarta.inject.Inject;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.service.notification.SecretListField;
import com.sequenceiq.cloudbreak.rotation.service.notification.SecretRotationNotificationService;
import com.sequenceiq.cloudbreak.rotation.service.status.DefaultSecretRotationStatusService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@Primary
@Component
public class FreeIpaSecretRotationStatusService extends DefaultSecretRotationStatusService {

    @Inject
    private StackService stackService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private SecretRotationNotificationService secretRotationNotificationService;

    @Override
    public void rotationStarted(String resourceCrn, SecretType secretType) {
        String secretTypeName = secretRotationNotificationService.getMessage(secretType, SecretListField.DISPLAY_NAME);
        updateStatus(resourceCrn, DetailedStackStatus.SECRET_ROTATION_STARTED, format("Secret rotation started: [%s]", secretTypeName));
    }

    @Override
    public void rotationFinished(String resourceCrn, SecretType secretType) {
        String secretTypeName = secretRotationNotificationService.getMessage(secretType, SecretListField.DISPLAY_NAME);
        updateStatus(resourceCrn, DetailedStackStatus.SECRET_ROTATION_FINISHED, format("Secret rotation finished: [%s]", secretTypeName));
    }

    @Override
    public void rotationFailed(String resourceCrn, SecretType secretType, String reason) {
        String secretTypeName = secretRotationNotificationService.getMessage(secretType, SecretListField.DISPLAY_NAME);
        Crn environmentCrn = Crn.safeFromString(resourceCrn);
        Stack stack = stackService.getByEnvironmentCrnAndAccountId(resourceCrn, environmentCrn.getAccountId());
        if (stack.getStackStatus().getDetailedStackStatus() != DetailedStackStatus.SECRET_ROTATION_ROLLBACK_FAILED
                && stack.getStackStatus().getDetailedStackStatus() != DetailedStackStatus.SECRET_ROTATION_FINALIZE_FAILED) {
            updateStatus(resourceCrn, DetailedStackStatus.SECRET_ROTATION_FAILED, format("Secret rotation failed: [%s], reason: [%s]", secretTypeName, reason));
        }
    }

    @Override
    public void rollbackStarted(String resourceCrn, SecretType secretType, String reason) {
        String secretTypeName = secretRotationNotificationService.getMessage(secretType, SecretListField.DISPLAY_NAME);
        updateStatus(resourceCrn, DetailedStackStatus.SECRET_ROTATION_ROLLBACK_STARTED,
                format("Secret rotation rollback started: [%s], reason: [%s]", secretTypeName, reason));
    }

    @Override
    public void rollbackFinished(String resourceCrn, SecretType secretType) {
        String secretTypeName = secretRotationNotificationService.getMessage(secretType, SecretListField.DISPLAY_NAME);
        updateStatus(resourceCrn, DetailedStackStatus.SECRET_ROTATION_ROLLBACK_FINISHED,
                format("Secret rotation rollback finished: [%s]", secretTypeName));
    }

    @Override
    public void rollbackFailed(String resourceCrn, SecretType secretType, String reason) {
        String secretTypeName = secretRotationNotificationService.getMessage(secretType, SecretListField.DISPLAY_NAME);
        updateStatus(resourceCrn, DetailedStackStatus.SECRET_ROTATION_ROLLBACK_FAILED,
                format("Secret rotation rollback failed: [%s], reason: [%s]", secretTypeName, reason));
    }

    @Override
    public void finalizeStarted(String resourceCrn, SecretType secretType) {
        String secretTypeName = secretRotationNotificationService.getMessage(secretType, SecretListField.DISPLAY_NAME);
        updateStatus(resourceCrn, DetailedStackStatus.SECRET_ROTATION_FINALIZE_STARTED,
                format("Secret rotation finalize started: [%s]", secretTypeName));
    }

    @Override
    public void finalizeFinished(String resourceCrn, SecretType secretType) {
        String secretTypeName = secretRotationNotificationService.getMessage(secretType, SecretListField.DISPLAY_NAME);
        updateStatus(resourceCrn, DetailedStackStatus.SECRET_ROTATION_FINALIZE_FINISHED,
                format("Secret rotation finalize finished: [%s]", secretTypeName));
    }

    @Override
    public void finalizeFailed(String resourceCrn, SecretType secretType, String reason) {
        String secretTypeName = secretRotationNotificationService.getMessage(secretType, SecretListField.DISPLAY_NAME);
        updateStatus(resourceCrn, DetailedStackStatus.SECRET_ROTATION_FINALIZE_FAILED,
                format("Secret rotation finalize failed: [%s], reason: [%s]", secretTypeName, reason));
    }

    @Override
    public void preVaildationFailed(String resourceCrn, SecretType secretType, String reason) {
        String secretTypeName = secretRotationNotificationService.getMessage(secretType, SecretListField.DISPLAY_NAME);
        updateStatus(resourceCrn, DetailedStackStatus.AVAILABLE,
                format("Secret rotation pre-validation failed: [%s], reason: [%s]", secretTypeName, reason));
    }

    private void updateStatus(String environmentCrnString, DetailedStackStatus secretRotationRollbackStarted, String statusReason) {
        Crn environmentCrn = Crn.safeFromString(environmentCrnString);
        Stack stack = stackService.getByEnvironmentCrnAndAccountId(environmentCrnString, environmentCrn.getAccountId());
        stackUpdater.updateStackStatus(stack, secretRotationRollbackStarted, statusReason);
    }

}
