package com.sequenceiq.datalake.service.sdx;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class SaltService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltService.class);

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private SdxReactorFlowManager sdxReactorFlowManager;

    public FlowIdentifier rotateSaltPassword(SdxCluster sdxCluster) {
        return sdxReactorFlowManager.triggerSaltPasswordRotationTracker(sdxCluster);
    }

    public FlowIdentifier updateSalt(SdxCluster sdxCluster) {
        SdxStatusEntity sdxStatus = sdxStatusService.getActualStatusForSdx(sdxCluster);
        DatalakeStatusEnum status = sdxStatus.getStatus();
        if (status.isStopState() || status.isDeleteInProgressOrCompleted()) {
            String message = String.format("SaltStack update cannot be initiated as datalake '%s' is currently in '%s' state.",
                    sdxCluster.getName(), status);
            LOGGER.info(message);
            throw new BadRequestException(message);
        } else {
            return sdxReactorFlowManager.triggerSaltUpdate(sdxCluster);
        }
    }
}
