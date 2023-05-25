package com.sequenceiq.datalake.service.sdx.status;

import javax.inject.Inject;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.flow.rotation.status.service.SecretRotationStatusService;

@Primary
@Component
public class SdxSecretRotationStatusService implements SecretRotationStatusService {

    @Inject
    private SdxStatusService sdxStatusService;

    @Override
    public void rotationStarted(String resourceCrn) {
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_IN_PROGRESS, null, resourceCrn);
    }

    @Override
    public void rotationFinished(String resourceCrn) {
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_FINISHED, null, resourceCrn);
    }

    @Override
    public void rotationFailed(String resourceCrn, String statusReason) {
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_FAILED, statusReason, resourceCrn);
    }
}
