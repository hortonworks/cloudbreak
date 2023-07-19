package com.sequenceiq.cloudbreak.service;

import javax.inject.Inject;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.service.status.SecretRotationStatusService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;

@Primary
@Component
public class StackSecretRotationStatusService implements SecretRotationStatusService {

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private StackDtoService stackDtoService;

    @Override
    public void rotationStarted(String resourceCrn, SecretType secretType) {
        stackUpdater.updateStackStatus(resourceCrn, DetailedStackStatus.SECRET_ROTATION_STARTED,
                String.format("Rotation started, secret type: %s", secretType.value()));
    }

    @Override
    public void rotationFinished(String resourceCrn, SecretType secretType) {
        stackUpdater.updateStackStatus(resourceCrn, DetailedStackStatus.SECRET_ROTATION_FINISHED,
                String.format("Rotation finished, secret type: %s", secretType.value()));
    }

    @Override
    public void rotationFailed(String resourceCrn, SecretType secretType, String reason) {
        StackView stack = stackDtoService.getStackViewByCrn(resourceCrn);
        if (stack.getStackStatus().getDetailedStackStatus() != DetailedStackStatus.SECRET_ROTATION_ROLLBACK_FAILED
                && stack.getStackStatus().getDetailedStackStatus() != DetailedStackStatus.SECRET_ROTATION_FINALIZE_FAILED) {
            stackUpdater.updateStackStatus(resourceCrn, DetailedStackStatus.SECRET_ROTATION_FAILED,
                    String.format("Rotation failed, secret type: %s, reason: %s", secretType.value(), reason));
        }
    }

    @Override
    public void rollbackStarted(String resourceCrn, SecretType secretType) {
        stackUpdater.updateStackStatus(resourceCrn, DetailedStackStatus.SECRET_ROTATION_ROLLBACK_STARTED,
                String.format("Rotation rollback started, secret type: %s", secretType.value()));
    }

    @Override
    public void rollbackFinished(String resourceCrn, SecretType secretType) {
        stackUpdater.updateStackStatus(resourceCrn, DetailedStackStatus.SECRET_ROTATION_ROLLBACK_FINISHED,
                String.format("Rotation rollback finished, secret type: %s", secretType.value()));
    }

    @Override
    public void rollbackFailed(String resourceCrn, SecretType secretType, String reason) {
        stackUpdater.updateStackStatus(resourceCrn, DetailedStackStatus.SECRET_ROTATION_ROLLBACK_FAILED,
                String.format("Rotation rollback failed, secret type: %s, reason: %s", secretType.value(), reason));
    }

    @Override
    public void finalizeStarted(String resourceCrn, SecretType secretType) {
        stackUpdater.updateStackStatus(resourceCrn, DetailedStackStatus.SECRET_ROTATION_FINALIZE_STARTED,
                String.format("Rotation finalize started, secret type: %s", secretType.value()));
    }

    @Override
    public void finalizeFinished(String resourceCrn, SecretType secretType) {
        stackUpdater.updateStackStatus(resourceCrn, DetailedStackStatus.SECRET_ROTATION_FINALIZE_FINISHED,
                String.format("Rotation finalize finished, secret type: %s", secretType.value()));
    }

    @Override
    public void finalizeFailed(String resourceCrn, SecretType secretType, String reason) {
        stackUpdater.updateStackStatus(resourceCrn, DetailedStackStatus.SECRET_ROTATION_FINALIZE_FAILED,
                String.format("Rotation finalize failed, secret type: %s, reason: %s", secretType.value(), reason));
    }
}
