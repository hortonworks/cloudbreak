package com.sequenceiq.datalake.flow;

import static com.sequenceiq.datalake.flow.delete.SdxDeleteEvent.SDX_DELETE_EVENT;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.flow.create.SdxCreateFlowConfig;
import com.sequenceiq.datalake.flow.datalake.recovery.DatalakeUpgradeRecoveryFlowConfig;
import com.sequenceiq.datalake.flow.datalake.upgrade.DatalakeUpgradeFlowConfig;
import com.sequenceiq.datalake.flow.delete.SdxDeleteFlowConfig;
import com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupFlowConfig;
import com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreFlowConfig;
import com.sequenceiq.datalake.flow.repair.SdxRepairFlowConfig;
import com.sequenceiq.datalake.flow.start.SdxStartFlowConfig;
import com.sequenceiq.datalake.flow.stop.SdxStopFlowConfig;
import com.sequenceiq.datalake.flow.upgrade.ccm.UpgradeCcmFlowConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.ApplicationFlowInformation;
import com.sequenceiq.flow.core.config.FlowConfiguration;
import com.sequenceiq.flow.domain.FlowLog;

@Component
public class SdxFlowInformation implements ApplicationFlowInformation {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxFlowInformation.class);

    private static final List<Class<? extends FlowConfiguration<?>>> RESTARTABLE_FLOWS = Arrays.asList(
            SdxCreateFlowConfig.class,
            SdxDeleteFlowConfig.class,
            SdxStartFlowConfig.class,
            SdxStopFlowConfig.class,
            SdxRepairFlowConfig.class,
            DatalakeUpgradeFlowConfig.class,
            DatalakeBackupFlowConfig.class,
            DatalakeRestoreFlowConfig.class,
            DatalakeUpgradeRecoveryFlowConfig.class,
            UpgradeCcmFlowConfig.class);

    private static final List<String> ALLOWED_PARALLEL_FLOWS = Collections.singletonList(SDX_DELETE_EVENT.event());

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxStatusService sdxStatusService;

    @Override
    public List<Class<? extends FlowConfiguration<?>>> getRestartableFlows() {
        return RESTARTABLE_FLOWS;
    }

    @Override
    public List<String> getAllowedParallelFlows() {
        return ALLOWED_PARALLEL_FLOWS;
    }

    @Override
    public List<Class<? extends FlowConfiguration<?>>> getTerminationFlow() {
        return Collections.singletonList(SdxDeleteFlowConfig.class);
    }

    @Override
    public void handleFlowFail(FlowLog flowLog) {
        SdxCluster sdxCluster = sdxService.getById(flowLog.getResourceId());
        if (sdxCluster != null)  {
            SdxStatusEntity actualStatusForSdx = sdxStatusService.getActualStatusForSdx(sdxCluster);
            if (actualStatusForSdx != null) {
                DatalakeStatusEnum status = actualStatusForSdx.getStatus();
                if (status != null) {
                    DatalakeStatusEnum failedStatus = status.mapToFailedIfInProgress();
                    try {
                        sdxStatusService.setStatusForDatalakeAndNotify(failedStatus, "Datalake flow failed", sdxCluster);
                    } catch (NotFoundException e) {
                        LOGGER.warn("We tried to handle flow fail, but can't set status to failed because data lake was not found. " +
                                "Probably another termination flow was terminate this data lake");
                    }
                }
            }
        }
    }
}
