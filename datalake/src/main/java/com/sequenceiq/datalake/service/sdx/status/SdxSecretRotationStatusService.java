package com.sequenceiq.datalake.service.sdx.status;

import jakarta.inject.Inject;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.service.notification.SecretListField;
import com.sequenceiq.cloudbreak.rotation.service.status.SecretRotationStatusService;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.service.sdx.SdxService;

@Primary
@Component
public class SdxSecretRotationStatusService implements SecretRotationStatusService {

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private SdxService sdxService;

    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Override
    public void rotationStarted(String resourceCrn, SecretType secretType) {
        String secretTypeName = cloudbreakMessagesService.getMessage(getCode(secretType));
        String statusReason = String.format("Rotation started, secret type: %s", secretTypeName);
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_IN_PROGRESS, statusReason, statusReason, resourceCrn);
    }

    @Override
    public void rotationFinished(String resourceCrn, SecretType secretType) {
        String secretTypeName = cloudbreakMessagesService.getMessage(getCode(secretType));
        String statusReason = String.format("Rotation finished, waiting for finalize, secret type: %s", secretTypeName);
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_FINISHED, statusReason, statusReason, resourceCrn);
    }

    @Override
    public void rotationFailed(String resourceCrn, SecretType secretType, String reason) {
        SdxCluster sdxCluster = sdxService.getByCrn(resourceCrn);
        SdxStatusEntity actualStatusForSdx = sdxStatusService.getActualStatusForSdx(sdxCluster);
        if (actualStatusForSdx.getStatus() != DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_ROLLBACK_FAILED
                && actualStatusForSdx.getStatus() != DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_FINALIZE_FAILED) {
            String secretTypeName = cloudbreakMessagesService.getMessage(getCode(secretType));
            String statusReason = String.format("Rotation failed, secret type: %s", secretTypeName);
            sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_FAILED, statusReason,
                    String.format("%s, reason: %s", statusReason, reason), resourceCrn);
        }
    }

    @Override
    public void rollbackStarted(String resourceCrn, SecretType secretType, String reason) {
        String secretTypeName = cloudbreakMessagesService.getMessage(getCode(secretType));
        String statusReason = String.format("Rotation rollback started, secret type: %s", secretTypeName);
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_ROLLBACK_IN_PROGRESS, statusReason,
                String.format("%s, reason: %s", statusReason, reason), resourceCrn);
    }

    @Override
    public void rollbackFinished(String resourceCrn, SecretType secretType) {
        String secretTypeName = cloudbreakMessagesService.getMessage(getCode(secretType));
        String statusReason = String.format("Rotation rollback finished, secret type: %s", secretTypeName);
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_ROLLBACK_FINISHED, statusReason, statusReason, resourceCrn);
    }

    @Override
    public void rollbackFailed(String resourceCrn, SecretType secretType, String reason) {
        String secretTypeName = cloudbreakMessagesService.getMessage(getCode(secretType));
        String statusReason = String.format("Rotation rollback failed, secret type: %s", secretTypeName);
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_ROLLBACK_FAILED, statusReason,
                String.format("%s, reason: %s", statusReason, reason), resourceCrn);
    }

    @Override
    public void finalizeStarted(String resourceCrn, SecretType secretType) {
        String secretTypeName = cloudbreakMessagesService.getMessage(getCode(secretType));
        String statusReason = String.format("Rotation finalize started, secret type: %s", secretTypeName);
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_FINALIZE_IN_PROGRESS, statusReason,
                statusReason, resourceCrn);
    }

    @Override
    public void finalizeFinished(String resourceCrn, SecretType secretType) {
        String secretTypeName = cloudbreakMessagesService.getMessage(getCode(secretType));
        String statusReason = String.format("Rotation finished, secret type: %s", secretTypeName);
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING, statusReason, statusReason, resourceCrn);
    }

    @Override
    public void finalizeFailed(String resourceCrn, SecretType secretType, String reason) {
        String secretTypeName = cloudbreakMessagesService.getMessage(getCode(secretType));
        String statusReason = String.format("Rotation finalize failed, secret type: %s", secretTypeName);
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_FINALIZE_FAILED, statusReason,
                String.format("%s, reason: %s", statusReason, reason), resourceCrn);
    }

    @Override
    public void preVaildationFailed(String resourceCrn) {
        SdxCluster sdxCluster = sdxService.getByCrn(resourceCrn);
        sdxStatusService.setStatusForDatalake(DatalakeStatusEnum.RUNNING, "", sdxCluster);
    }

    private String getCode(SecretType secretType) {
        return secretType.getClazz().getSimpleName() + "." + SecretListField.DISPLAY_NAME.name() + "." + secretType.value();
    }
}
