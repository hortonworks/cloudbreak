package com.sequenceiq.cloudbreak.reactor.handler.cluster.dr.backup;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.datalakedr.DatalakeDrClient;
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeDrStatusResponse;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.backup.DatalakeBackupSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.backup.DatalakeBackupFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.backup.FullBackupStatusRequest;
import com.sequenceiq.cloudbreak.service.externaldatabase.PollingConfig;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;

import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FullBackupStatusHandler extends ExceptionCatcherEventHandler<FullBackupStatusRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FullBackupStatusHandler.class);

    @Value("${sdx.stack.backup.status.sleeptime_sec:20}")
    private int sleepTimeInSec;

    @Value("${sdx.stack.backup.status.duration_min:90}")
    private int durationInMinutes;

    @Inject
    private DatalakeDrClient datalakeDrClient;

    @Inject
    private StackService stackService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(FullBackupStatusRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e) {
        return new DatalakeBackupFailedEvent(resourceId, e, DetailedStackStatus.FULL_BACKUP_FAILED);
    }

    @Override
    protected Selectable doAccept(HandlerEvent event) {
        LOGGER.info("HER in FullBackupStatusHandler");
        FullBackupStatusRequest request = event.getData();
        Selectable result;
        Long stackId = request.getResourceId();
        LOGGER.debug("Checking status of backup on stack {}, backup id {}", stackId, request.getBackupId());
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(stackId);
            Cluster cluster = stack.getCluster();

            DatalakeDrStatusResponse response = pollForFinalBackupStatus(cluster.getName(), request.getBackupId(),
                request.getUserCrn());
            LOGGER.info("HER response.getState() " + response.getState());
            if (response.getState() == DatalakeDrStatusResponse.State.FAILED) {
                result = new DatalakeBackupFailedEvent(stackId, new Exception("Datalake backup failed: " + response.getFailureReason()),
                    DetailedStackStatus.FULL_BACKUP_FAILED);
            } else {
                result = new DatalakeBackupSuccess(stackId);
            }
        } catch (Exception e) {
            LOGGER.error("Unable to fetch status of full datalake backup", e);
            result = new DatalakeBackupFailedEvent(stackId, e, DetailedStackStatus.FULL_BACKUP_FAILED);
        }
        return result;
    }

    private DatalakeDrStatusResponse pollForFinalBackupStatus(String datalakeName, String backupId, String userCrn) {
        LOGGER.debug("Starting backup status poller for datalake {}", datalakeName);
        LOGGER.info("HER Starting backup status poller for datalake {}", datalakeName);

        PollingConfig pollingConfig = PollingConfig.builder()
            .withSleepTime(sleepTimeInSec)
            .withSleepTimeUnit(TimeUnit.SECONDS)
            .withTimeout(durationInMinutes)
            .withTimeoutTimeUnit(TimeUnit.MINUTES)
            .withStopPollingIfExceptionOccured(true)
            .build();

        return Polling.waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
            .stopIfException(pollingConfig.getStopPollingIfExceptionOccured())
            .stopAfterDelay(pollingConfig.getTimeout(), pollingConfig.getTimeoutTimeUnit())
            .run(() -> getBackupStatusAttemptResult(datalakeName, backupId, userCrn));
    }

    private AttemptResult<DatalakeDrStatusResponse> getBackupStatusAttemptResult(String datalakeName, String backupId, String userCrn) {
        DatalakeDrStatusResponse response = datalakeDrClient.getBackupStatusByBackupId(datalakeName, backupId, userCrn);
        if (!response.isComplete()) {
            LOGGER.debug("Datalake {} backup still in progress", datalakeName);
            LOGGER.info("HER Datalake {} backup still in progress", datalakeName);
            return AttemptResults.justContinue();
        } else {
            LOGGER.debug("Datalake {} backup complete with status {}", datalakeName, response.getState());
            LOGGER.info("HER Datalake {} backup complete with status {}", datalakeName, response.getState());
            return AttemptResults.finishWith(response);
        }
    }
}
