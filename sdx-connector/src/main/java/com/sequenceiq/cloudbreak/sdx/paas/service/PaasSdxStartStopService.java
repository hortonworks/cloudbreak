package com.sequenceiq.cloudbreak.sdx.paas.service;

import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.RUNNING;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.START_IN_PROGRESS;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.STOPPED;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.STOP_IN_PROGRESS;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.core.AttemptMaker;
import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxStartStopService;
import com.sequenceiq.cloudbreak.sdx.paas.flowpolling.FlowPollingService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@Service
public class PaasSdxStartStopService extends AbstractPaasSdxService implements PlatformAwareSdxStartStopService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PaasSdxStartStopService.class);

    private static final Set<SdxClusterStatusResponse> SKIP_STOP_OPERATION = Set.of(STOPPED, STOP_IN_PROGRESS);

    private static final Set<SdxClusterStatusResponse> SKIP_START_OPERATION = Set.of(RUNNING, START_IN_PROGRESS);

    @Value("${env.stop.polling.attempt:360}")
    private Integer maxPollAttempt;

    @Value("${env.stop.polling.sleep.time:5}")
    private Integer pollSleepTime;

    @Inject
    private SdxEndpoint sdxEndpoint;

    @Inject
    private FlowPollingService flowPollingService;

    @Override
    public void startSdx(String sdxCrn) {
        SdxClusterResponse sdx = ThreadBasedUserCrnProvider.doAsInternalActor(() -> sdxEndpoint.getByCrn(sdxCrn));
        if (SKIP_START_OPERATION.contains(sdx.getStatus())) {
            LOGGER.info("No need to call start for SDX {} as it is already running or starting.", sdxCrn);
            return;
        }

        LOGGER.info("Calling start for SDX PaaS cluster {}", sdxCrn);
        FlowIdentifier flowId = ThreadBasedUserCrnProvider.doAsInternalActor(() -> sdxEndpoint.startByCrn(sdxCrn));

        LOGGER.info("Polling start for SDX PaaS cluster {}", sdxCrn);
        pollOperation(() -> flowPollingService.pollFlowIdAndReturnAttemptResult(flowId));
    }

    @Override
    public void stopSdx(String sdxCrn) {
        SdxClusterResponse sdx = ThreadBasedUserCrnProvider.doAsInternalActor(() -> sdxEndpoint.getByCrn(sdxCrn));
        if (SKIP_STOP_OPERATION.contains(sdx.getStatus())) {
            LOGGER.info("No need to call stop for SDX {} as it is already stopped or stopping.", sdxCrn);
            return;
        }

        LOGGER.info("Calling stop for SDX PaaS cluster {}", sdxCrn);
        FlowIdentifier flowId = ThreadBasedUserCrnProvider.doAsInternalActor(() -> sdxEndpoint.stopByCrn(sdxCrn));

        LOGGER.info("Polling stop for SDX PaaS cluster {}", sdxCrn);
        pollOperation(() -> flowPollingService.pollFlowIdAndReturnAttemptResult(flowId));
    }

    private void pollOperation(AttemptMaker<Object> pollingAttempt) {
        try {
            Polling.stopAfterAttempt(maxPollAttempt)
                    .stopIfException(false)
                    .waitPeriodly(pollSleepTime, TimeUnit.SECONDS)
                    .run(pollingAttempt);
        } catch (PollerStoppedException e) {
            LOGGER.error("Polling for datalake start/stop timed out", e);
            throw new RuntimeException("Datalake start/stop timed out", e);
        }
    }
}
