package com.sequenceiq.cloudbreak.sdx.cdl.service;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto;
import com.dyngr.Polling;
import com.dyngr.core.AttemptMaker;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.cloudbreak.sdx.cdl.grpc.GrpcSdxCdlClient;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxStartStopService;

@Service
public class CdlSdxStartStopService extends AbstractCdlSdxService implements PlatformAwareSdxStartStopService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CdlSdxStartStopService.class);

    @Value("${env.stop.polling.attempt:360}")
    private Integer maxPollAttempt;

    @Value("${env.stop.polling.sleep.time:5}")
    private Integer pollSleepTime;

    @Inject
    private GrpcSdxCdlClient grpcClient;

    @Override
    public void startSdx(String sdxCrn) {
        if (isEnabled(sdxCrn)) {
            try {
                grpcClient.startDatalake(sdxCrn);
            } catch (RuntimeException exception) {
                LOGGER.error(String.format("Unable to start CDL with CRN: %s.", sdxCrn), exception);
                return;
            }
            pollOperation(() -> getPollingResultForStart(sdxCrn));
        }
    }

    @Override
    public void stopSdx(String sdxCrn) {
        if (isEnabled(sdxCrn)) {
            try {
                grpcClient.stopDatalake(sdxCrn);
            } catch (RuntimeException exception) {
                LOGGER.error(String.format("Unable to stop CDL with CRN: %s.", sdxCrn), exception);
                return;
            }
            pollOperation(() -> getPollingResultForStop(sdxCrn));
        }
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

    private AttemptResult<Object> getPollingResultForStart(String sdxCrn) {
        return switch (getCdlStatus(sdxCrn)) {
            case RUNNING -> AttemptResults.finishWith(null);
            case UNAVAILABLE -> AttemptResults.breakFor(new IllegalStateException("CDL is in unavailable state. Start failed for CDL with CRN: " + sdxCrn));
            default -> AttemptResults.justContinue();
        };
    }

    private AttemptResult<Object> getPollingResultForStop(String sdxCrn) {
        return switch (getCdlStatus(sdxCrn)) {
            case STOPPED -> AttemptResults.finishWith(null);
            case UNAVAILABLE -> AttemptResults.breakFor(new IllegalStateException("CDL is in unavailable state. Start failed for CDL with CRN: " + sdxCrn));
            default -> AttemptResults.justContinue();
        };
    }

    private CdlCrudProto.StatusType.Value getCdlStatus(String sdxCrn) {
        try {
        return grpcClient.describeDatalake(sdxCrn).getStatus();
        } catch (RuntimeException e) {
            LOGGER.error(String.format("Failed to get status for CDL with CRN: %s", sdxCrn), e);
            return CdlCrudProto.StatusType.Value.UNAVAILABLE;
        }
    }
}
