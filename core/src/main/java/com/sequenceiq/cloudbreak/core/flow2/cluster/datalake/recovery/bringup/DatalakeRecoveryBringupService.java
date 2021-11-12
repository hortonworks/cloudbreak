package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.recovery.bringup;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_RECOVERY_BRINGUP_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_RECOVERY_BRINGUP_FINISHED;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.service.StackUpdater;

@Service
public class DatalakeRecoveryBringupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeRecoveryBringupService.class);

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private StackUpdater stackUpdater;

    public void handleDatalakeRecoveryBringupSuccess(Long stackId) {
        LOGGER.info("Datalake stack bringup has been finished successfully.");
        flowMessageService.fireEventAndLog(stackId, Status.AVAILABLE.name(), DATALAKE_RECOVERY_BRINGUP_FINISHED);
    }

    public void handleDatalakeRecoveryBringupFailure(Long stackId, String errorReason, DetailedStackStatus detailedStatus) {
        LOGGER.warn("Datalake stack bringup has failed with {}.", errorReason);
        stackUpdater.updateStackStatus(stackId, detailedStatus, errorReason);
        flowMessageService.fireEventAndLog(stackId, Status.UPDATE_FAILED.name(), DATALAKE_RECOVERY_BRINGUP_FAILED, errorReason);
    }

}
