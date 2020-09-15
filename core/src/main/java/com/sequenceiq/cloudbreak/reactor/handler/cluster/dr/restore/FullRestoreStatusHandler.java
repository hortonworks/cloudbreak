package com.sequenceiq.cloudbreak.reactor.handler.cluster.dr.restore;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.datalakedr.DatalakeDrClient;
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeDrStatusResponse;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.restore.DatalakeRestoreFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.restore.DatalakeRestoreSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.dr.restore.FullRestoreStatusRequest;
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
public class FullRestoreStatusHandler extends ExceptionCatcherEventHandler<FullRestoreStatusRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FullRestoreStatusHandler.class);

    @Value("${sdx.stack.restore.status.sleeptime_sec:20}")
    private int sleepTimeInSec;

    @Value("${sdx.stack.restore.status.duration_min:90}")
    private int durationInMinutes;

    @Inject
    private DatalakeDrClient datalakeDrClient;

    @Inject
    private StackService stackService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(FullRestoreStatusRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e) {
        return new DatalakeRestoreFailedEvent(resourceId, e, DetailedStackStatus.FULL_RESTORE_FAILED);
    }

    @Override
    protected Selectable doAccept(HandlerEvent event) {
        LOGGER.info("HER in FullRestoreStatusHandler");
        FullRestoreStatusRequest request = event.getData();
        Selectable result;
        Long stackId = request.getResourceId();
        LOGGER.debug("Checking status of restore on stack {}, restore id {}", stackId, request.getBackupId());
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(stackId);
            Cluster cluster = stack.getCluster();

            DatalakeDrStatusResponse response = pollForFinalRestoreStatus(cluster.getName(), request.getBackupId(),
                request.getUserCrn());
            LOGGER.info("HER response.getState() " + response.getState());
            if (response.getState() == DatalakeDrStatusResponse.State.FAILED) {
                result = new DatalakeRestoreFailedEvent(stackId, new Exception("Datalake restore failed: " + response.getFailureReason()),
                    DetailedStackStatus.FULL_RESTORE_FAILED);
            } else {
                result = new DatalakeRestoreSuccess(stackId);
            }
        } catch (Exception e) {
            LOGGER.error("Unable to fetch status of full datalake restore", e);
            result = new DatalakeRestoreFailedEvent(stackId, e, DetailedStackStatus.FULL_RESTORE_FAILED);
        }
        return result;
    }

    private DatalakeDrStatusResponse pollForFinalRestoreStatus(String datalakeName, String restoreId, String userCrn) {
        LOGGER.debug("Starting restore status poller for datalake {}", datalakeName);
        LOGGER.info("HER Starting restore status poller for datalake {}", datalakeName);

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
            .run(() -> getRestoreStatusAttemptResult(datalakeName, restoreId, userCrn));
    }

    private AttemptResult<DatalakeDrStatusResponse> getRestoreStatusAttemptResult(String datalakeName, String restoreId, String userCrn) {
        DatalakeDrStatusResponse response = datalakeDrClient.getRestoreStatusByRestoreId(datalakeName, restoreId, userCrn);
        if (!response.isComplete()) {
            LOGGER.debug("Datalake {} restore still in progress", datalakeName);
            LOGGER.info("HER Datalake {} restore still in progress", datalakeName);
            return AttemptResults.justContinue();
        } else {
            LOGGER.debug("Datalake {} restore complete with status {}", datalakeName, response.getState());
            LOGGER.info("HER Datalake {} restore complete with status {}", datalakeName, response.getState());
            return AttemptResults.finishWith(response);
        }
    }
}
