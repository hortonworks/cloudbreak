package com.sequenceiq.datalake.service.rotation;

import javax.inject.Inject;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.service.status.SecretRotationStatusService;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;

@Primary
@Component
public class SdxSecretRotationStatusService implements SecretRotationStatusService {

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private SdxService sdxService;

    @Override
    public void rotationStarted(String resourceCrn) {
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_IN_PROGRESS, null, resourceCrn);
    }

    @Override
    public void rotationFinished(String resourceCrn) {
        SdxCluster sdxCluster = sdxService.getByCrn(resourceCrn);
        SdxStatusEntity actualStatusForSdx = sdxStatusService.getActualStatusForSdx(sdxCluster);
        if (actualStatusForSdx.getStatus() != DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_FAILED) {
            sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_FINISHED, null, resourceCrn);
        }
    }

    @Override
    public void rotationFailed(String resourceCrn, String statusReason) {
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_FAILED, statusReason, resourceCrn);
    }
}
