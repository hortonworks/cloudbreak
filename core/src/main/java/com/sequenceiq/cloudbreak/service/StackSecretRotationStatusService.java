package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.redbeams.api.model.common.Status.UPDATE_IN_PROGRESS;

import javax.inject.Inject;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.rotation.SecretType;
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

    @Override
    public void rotationStarted(String resourceCrn, SecretType secretType) {
        stackUpdater.updateStackStatus(resourceCrn, DetailedStackStatus.SECRET_ROTATION_STARTED,
                String.format("Rotation started, secret type: %s", secretType.value()));
        StackView stack = stackDtoService.getStackViewByCrn(resourceCrn);
        fireCloudbreakEvent(stack, ResourceEvent.SECRET_ROTATION_IN_PROGRESS);
    }

    @Override
    public void rotationFinished(String resourceCrn, SecretType secretType) {
        stackUpdater.updateStackStatus(resourceCrn, DetailedStackStatus.SECRET_ROTATION_FINISHED,
                String.format("Rotation finished, secret type: %s", secretType.value()));
        StackView stack = stackDtoService.getStackViewByCrn(resourceCrn);
        fireCloudbreakEvent(stack, ResourceEvent.SECRET_ROTATION_FINISHED);
    }

    @Override
    public void rotationFailed(String resourceCrn, SecretType secretType, String reason) {
        StackView stack = stackDtoService.getStackViewByCrn(resourceCrn);
        if (stack.getStackStatus().getDetailedStackStatus() != DetailedStackStatus.SECRET_ROTATION_ROLLBACK_FAILED
                && stack.getStackStatus().getDetailedStackStatus() != DetailedStackStatus.SECRET_ROTATION_FINALIZE_FAILED) {
            stackUpdater.updateStackStatus(resourceCrn, DetailedStackStatus.SECRET_ROTATION_FAILED,
                    String.format("Rotation failed, secret type: %s, reason: %s", secretType.value(), reason));
            fireCloudbreakEvent(stack, ResourceEvent.SECRET_ROTATION_FAILED);
        }
    }

    @Override
    public void rollbackStarted(String resourceCrn, SecretType secretType) {
        stackUpdater.updateStackStatus(resourceCrn, DetailedStackStatus.SECRET_ROTATION_ROLLBACK_STARTED,
                String.format("Rotation rollback started, secret type: %s", secretType.value()));
        StackView stack = stackDtoService.getStackViewByCrn(resourceCrn);
        fireCloudbreakEvent(stack, ResourceEvent.SECRET_ROTATION_ROLLBACK_IN_PROGRESS);
    }

    @Override
    public void rollbackFinished(String resourceCrn, SecretType secretType) {
        stackUpdater.updateStackStatus(resourceCrn, DetailedStackStatus.SECRET_ROTATION_ROLLBACK_FINISHED,
                String.format("Rotation rollback finished, secret type: %s", secretType.value()));
        StackView stack = stackDtoService.getStackViewByCrn(resourceCrn);
        fireCloudbreakEvent(stack, ResourceEvent.SECRET_ROTATION_ROLLBACK_FINISHED);
    }

    @Override
    public void rollbackFailed(String resourceCrn, SecretType secretType, String reason) {
        stackUpdater.updateStackStatus(resourceCrn, DetailedStackStatus.SECRET_ROTATION_ROLLBACK_FAILED,
                String.format("Rotation rollback failed, secret type: %s, reason: %s", secretType.value(), reason));
        StackView stack = stackDtoService.getStackViewByCrn(resourceCrn);
        fireCloudbreakEvent(stack, ResourceEvent.SECRET_ROTATION_ROLLBACK_FAILED);
    }

    @Override
    public void finalizeStarted(String resourceCrn, SecretType secretType) {
        stackUpdater.updateStackStatus(resourceCrn, DetailedStackStatus.SECRET_ROTATION_FINALIZE_STARTED,
                String.format("Rotation finalize started, secret type: %s", secretType.value()));
        StackView stack = stackDtoService.getStackViewByCrn(resourceCrn);
        fireCloudbreakEvent(stack, ResourceEvent.SECRET_ROTATION_FINALIZE_IN_PROGRESS);
    }

    @Override
    public void finalizeFinished(String resourceCrn, SecretType secretType) {
        stackUpdater.updateStackStatus(resourceCrn, DetailedStackStatus.SECRET_ROTATION_FINALIZE_FINISHED,
                String.format("Rotation finalize finished, secret type: %s", secretType.value()));
        StackView stack = stackDtoService.getStackViewByCrn(resourceCrn);
        fireCloudbreakEvent(stack, ResourceEvent.SECRET_ROTATION_FINALIZE_FINISHED);
    }

    @Override
    public void finalizeFailed(String resourceCrn, SecretType secretType, String reason) {
        stackUpdater.updateStackStatus(resourceCrn, DetailedStackStatus.SECRET_ROTATION_FINALIZE_FAILED,
                String.format("Rotation finalize failed, secret type: %s, reason: %s", secretType.value(), reason));
        StackView stack = stackDtoService.getStackViewByCrn(resourceCrn);
        fireCloudbreakEvent(stack, ResourceEvent.SECRET_ROTATION_FINALIZE_FAILED);
    }

    private void fireCloudbreakEvent(StackView stack, ResourceEvent event) {
        cloudbreakEventService.fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), event);
    }
}
