package com.sequenceiq.datalake.service.sdx.status;

import static java.lang.String.format;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.service.notification.SecretListField;
import com.sequenceiq.cloudbreak.rotation.service.notification.SecretRotationNotificationService;
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
    private SecretRotationNotificationService secretRotationNotificationService;

    @Override
    public void rotationStarted(String resourceCrn, SecretType secretType) {
        String secretTypeName = secretRotationNotificationService.getMessage(secretType, SecretListField.DISPLAY_NAME);
        SdxCluster sdxCluster = sdxService.getByCrn(resourceCrn);
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_IN_PROGRESS,
                List.of(secretTypeName), format("Secret rotation started: %s", secretTypeName), sdxCluster);
    }

    @Override
    public void rotationFinished(String resourceCrn, SecretType secretType) {
        String secretTypeName = secretRotationNotificationService.getMessage(secretType, SecretListField.DISPLAY_NAME);
        SdxCluster sdxCluster = sdxService.getByCrn(resourceCrn);
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_FINISHED,
                List.of(secretTypeName), format("Secret rotation finished: %s", secretTypeName), sdxCluster);
    }

    @Override
    public void rotationFailed(String resourceCrn, SecretType secretType, String reason) {
        SdxCluster sdxCluster = sdxService.getByCrn(resourceCrn);
        SdxStatusEntity actualStatusForSdx = sdxStatusService.getActualStatusForSdx(sdxCluster);
        if (actualStatusForSdx.getStatus() != DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_ROLLBACK_FAILED
                && actualStatusForSdx.getStatus() != DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_FINALIZE_FAILED) {
            String secretTypeName = secretRotationNotificationService.getMessage(secretType, SecretListField.DISPLAY_NAME);
            sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_FAILED,
                    List.of(secretTypeName, reason), format("Secret rotation failed: %s", secretTypeName), sdxCluster);
        }
    }

    @Override
    public void rollbackStarted(String resourceCrn, SecretType secretType, String reason) {
        String secretTypeName = secretRotationNotificationService.getMessage(secretType, SecretListField.DISPLAY_NAME);
        SdxCluster sdxCluster = sdxService.getByCrn(resourceCrn);
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_ROLLBACK_IN_PROGRESS,
                List.of(secretTypeName, reason), format("Secret rotation rollback started: %s", secretTypeName), sdxCluster);
    }

    @Override
    public void rollbackFinished(String resourceCrn, SecretType secretType) {
        String secretTypeName = secretRotationNotificationService.getMessage(secretType, SecretListField.DISPLAY_NAME);
        SdxCluster sdxCluster = sdxService.getByCrn(resourceCrn);
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_ROLLBACK_FINISHED,
                List.of(secretTypeName), format("Secret rotation rollback finished: %s", secretTypeName), sdxCluster);
    }

    @Override
    public void rollbackFailed(String resourceCrn, SecretType secretType, String reason) {
        String secretTypeName = secretRotationNotificationService.getMessage(secretType, SecretListField.DISPLAY_NAME);
        SdxCluster sdxCluster = sdxService.getByCrn(resourceCrn);
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_ROLLBACK_FAILED,
                List.of(secretTypeName, reason), format("Secret rotation rollback failed: %s", secretTypeName), sdxCluster);
    }

    @Override
    public void finalizeStarted(String resourceCrn, SecretType secretType) {
        String secretTypeName = secretRotationNotificationService.getMessage(secretType, SecretListField.DISPLAY_NAME);
        SdxCluster sdxCluster = sdxService.getByCrn(resourceCrn);
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_FINALIZE_IN_PROGRESS,
                List.of(secretTypeName), format("Secret rotation finalize started: %s", secretTypeName), sdxCluster);
    }

    @Override
    public void finalizeFinished(String resourceCrn, SecretType secretType) {
        String secretTypeName = secretRotationNotificationService.getMessage(secretType, SecretListField.DISPLAY_NAME);
        SdxCluster sdxCluster = sdxService.getByCrn(resourceCrn);
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING, ResourceEvent.SECRET_ROTATION_FINALIZE_FINISHED,
                List.of(secretTypeName), format("Secret rotation finalize finished: %s", secretTypeName), sdxCluster);
    }

    @Override
    public void finalizeFailed(String resourceCrn, SecretType secretType, String reason) {
        String secretTypeName = secretRotationNotificationService.getMessage(secretType, SecretListField.DISPLAY_NAME);
        SdxCluster sdxCluster = sdxService.getByCrn(resourceCrn);
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_FINALIZE_FAILED,
                List.of(secretTypeName, reason), format("Secret rotation finalize failed: %s", secretTypeName), sdxCluster);
    }

    @Override
    public void preVaildationFailed(String resourceCrn, SecretType secretType, String reason) {
        String secretTypeName = secretRotationNotificationService.getMessage(secretType, SecretListField.DISPLAY_NAME);
        SdxCluster sdxCluster = sdxService.getByCrn(resourceCrn);
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING, ResourceEvent.SECRET_ROTATION_PREVALIDATE_FAILED,
                List.of(secretTypeName, reason), format("Secret rotation pre-validation failed: %s", secretTypeName), sdxCluster);
    }
}
