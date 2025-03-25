package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.redbeams.api.model.common.Status.UPDATE_FAILED;
import static com.sequenceiq.redbeams.api.model.common.Status.UPDATE_IN_PROGRESS;
import static java.lang.String.format;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.service.notification.SecretListField;
import com.sequenceiq.cloudbreak.rotation.service.notification.SecretRotationNotificationService;
import com.sequenceiq.cloudbreak.rotation.service.status.SecretRotationStatusService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.view.StackView;

@Primary
@Component
public class StackSecretRotationStatusService implements SecretRotationStatusService {

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Inject
    private SecretRotationNotificationService secretRotationNotificationService;

    @Override
    public void rotationStarted(String resourceCrn, SecretType secretType) {
        String secretTypeName = secretRotationNotificationService.getMessage(secretType, SecretListField.DISPLAY_NAME);
        stackUpdater.updateStackStatus(resourceCrn, DetailedStackStatus.SECRET_ROTATION_STARTED,
                format("Secret rotation started: %s", secretTypeName));
        StackView stack = stackDtoService.getStackViewByCrn(resourceCrn);
        if (stack.isDatahub()) {
            cloudbreakEventService.fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.SECRET_ROTATION_IN_PROGRESS,
                    List.of(secretTypeName));
        }
    }

    @Override
    public void rotationFinished(String resourceCrn, SecretType secretType) {
        String secretTypeName = secretRotationNotificationService.getMessage(secretType, SecretListField.DISPLAY_NAME);
        stackUpdater.updateStackStatus(resourceCrn, DetailedStackStatus.SECRET_ROTATION_FINISHED,
                format("Secret rotation finished: %s", secretTypeName));
        StackView stack = stackDtoService.getStackViewByCrn(resourceCrn);
        if (stack.isDatahub()) {
            cloudbreakEventService.fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.SECRET_ROTATION_FINISHED,
                    List.of(secretTypeName));
        }
    }

    @Override
    public void rotationFailed(String resourceCrn, SecretType secretType, String reason) {
        StackView stack = stackDtoService.getStackViewByCrn(resourceCrn);
        if (stack.getStackStatus().getDetailedStackStatus() != DetailedStackStatus.SECRET_ROTATION_ROLLBACK_FAILED
                && stack.getStackStatus().getDetailedStackStatus() != DetailedStackStatus.SECRET_ROTATION_FINALIZE_FAILED) {
            String secretTypeName = secretRotationNotificationService.getMessage(secretType, SecretListField.DISPLAY_NAME);
            stackUpdater.updateStackStatus(resourceCrn, DetailedStackStatus.SECRET_ROTATION_FAILED,
                    format("Secret rotation failed: %s", secretTypeName));
            if (stack.isDatahub()) {
                cloudbreakEventService.fireCloudbreakEvent(stack.getId(), UPDATE_FAILED.name(), ResourceEvent.SECRET_ROTATION_FAILED,
                        List.of(secretTypeName, reason));
            }
        }
    }

    @Override
    public void rollbackStarted(String resourceCrn, SecretType secretType, String reason) {
        String secretTypeName = secretRotationNotificationService.getMessage(secretType, SecretListField.DISPLAY_NAME);
        stackUpdater.updateStackStatus(resourceCrn, DetailedStackStatus.SECRET_ROTATION_ROLLBACK_STARTED,
                format("Secret rotation rollback started: %s", secretTypeName));
        StackView stack = stackDtoService.getStackViewByCrn(resourceCrn);
        if (stack.isDatahub()) {
            cloudbreakEventService.fireCloudbreakEvent(stack.getId(), UPDATE_FAILED.name(), ResourceEvent.SECRET_ROTATION_ROLLBACK_IN_PROGRESS,
                    List.of(secretTypeName, reason));
        }
    }

    @Override
    public void rollbackFinished(String resourceCrn, SecretType secretType) {
        String secretTypeName = secretRotationNotificationService.getMessage(secretType, SecretListField.DISPLAY_NAME);
        stackUpdater.updateStackStatus(resourceCrn, DetailedStackStatus.SECRET_ROTATION_ROLLBACK_FINISHED,
                format("Secret rotation rollback finished: %s", secretTypeName));
        StackView stack = stackDtoService.getStackViewByCrn(resourceCrn);
        if (stack.isDatahub()) {
            cloudbreakEventService.fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.SECRET_ROTATION_ROLLBACK_FINISHED,
                    List.of(secretTypeName));
        }
    }

    @Override
    public void rollbackFailed(String resourceCrn, SecretType secretType, String reason) {
        String secretTypeName = secretRotationNotificationService.getMessage(secretType, SecretListField.DISPLAY_NAME);
        stackUpdater.updateStackStatus(resourceCrn, DetailedStackStatus.SECRET_ROTATION_ROLLBACK_FAILED,
                format("Secret rotation rollback failed: %s", secretTypeName));
        StackView stack = stackDtoService.getStackViewByCrn(resourceCrn);
        if (stack.isDatahub()) {
            cloudbreakEventService.fireCloudbreakEvent(stack.getId(), UPDATE_FAILED.name(), ResourceEvent.SECRET_ROTATION_ROLLBACK_FAILED,
                    List.of(secretTypeName, reason));
        }
    }

    @Override
    public void finalizeStarted(String resourceCrn, SecretType secretType) {
        String secretTypeName = secretRotationNotificationService.getMessage(secretType, SecretListField.DISPLAY_NAME);
        stackUpdater.updateStackStatus(resourceCrn, DetailedStackStatus.SECRET_ROTATION_FINALIZE_STARTED,
                format("Secret rotation finalize started: %s", secretTypeName));
        StackView stack = stackDtoService.getStackViewByCrn(resourceCrn);
        if (stack.isDatahub()) {
            cloudbreakEventService.fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.SECRET_ROTATION_FINALIZE_IN_PROGRESS,
                    List.of(secretTypeName));
        }
    }

    @Override
    public void finalizeFinished(String resourceCrn, SecretType secretType) {
        String secretTypeName = secretRotationNotificationService.getMessage(secretType, SecretListField.DISPLAY_NAME);
        stackUpdater.updateStackStatus(resourceCrn, DetailedStackStatus.SECRET_ROTATION_FINALIZE_FINISHED,
                format("Secret rotation finalize finished: %s", secretTypeName));
        StackView stack = stackDtoService.getStackViewByCrn(resourceCrn);
        if (stack.isDatahub()) {
            cloudbreakEventService.fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), ResourceEvent.SECRET_ROTATION_FINALIZE_FINISHED,
                    List.of(secretTypeName));
        }
    }

    @Override
    public void finalizeFailed(String resourceCrn, SecretType secretType, String reason) {
        String secretTypeName = secretRotationNotificationService.getMessage(secretType, SecretListField.DISPLAY_NAME);
        stackUpdater.updateStackStatus(resourceCrn, DetailedStackStatus.SECRET_ROTATION_FINALIZE_FAILED,
                format("Secret rotation finalize failed: %s", secretTypeName));
        StackView stack = stackDtoService.getStackViewByCrn(resourceCrn);
        if (stack.isDatahub()) {
            cloudbreakEventService.fireCloudbreakEvent(stack.getId(), UPDATE_FAILED.name(), ResourceEvent.SECRET_ROTATION_FINALIZE_FAILED,
                    List.of(secretTypeName, reason));
        }
    }

    @Override
    public void preVaildationFailed(String resourceCrn, SecretType secretType, String reason) {
        String secretTypeName = secretRotationNotificationService.getMessage(secretType, SecretListField.DISPLAY_NAME);
        stackUpdater.updateStackStatus(resourceCrn, DetailedStackStatus.SECRET_ROTATION_PREVALIDATION_FAILED,
                format("Secret rotation pre-validation failed: %s", secretTypeName));
        StackView stack = stackDtoService.getStackViewByCrn(resourceCrn);
        if (stack.isDatahub()) {
            cloudbreakEventService.fireCloudbreakEvent(stack.getId(), UPDATE_FAILED.name(), ResourceEvent.SECRET_ROTATION_PREVALIDATE_FAILED,
                    List.of(secretTypeName, reason));
        }
    }
}
