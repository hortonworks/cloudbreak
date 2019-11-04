package com.sequenceiq.environment.environment.service;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

@Service
public class FreeipaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeipaService.class);

    @Value("${env.stop.polling.attempt:90}")
    private Integer attempt;

    @Value("${env.stop.polling.sleep.time:5}")
    private Integer sleeptime;

    private final FreeIpaV1Endpoint freeIpaV1Endpoint;

    public FreeipaService(FreeIpaV1Endpoint freeIpaV1Endpoint) {
        this.freeIpaV1Endpoint = freeIpaV1Endpoint;
    }

    public void stopAttachedFreeipa(String envCrn) {
        freeIpaV1Endpoint.stop(envCrn);
        Polling.stopAfterAttempt(attempt)
                .stopIfException(true)
                .waitPeriodly(sleeptime, TimeUnit.SECONDS)
                .run(() -> {
                    DescribeFreeIpaResponse freeIpaResponse = freeIpaV1Endpoint.describe(envCrn);
                    if (freeipaStopped(freeIpaResponse)) {
                        return AttemptResults.finishWith(null);
                    } else {
                        return checkStopStatus(freeIpaResponse);
                    }
                });

    }

    public void startAttachedFreeipa(String envCrn) {
        freeIpaV1Endpoint.start(envCrn);
        Polling.stopAfterAttempt(attempt)
                .stopIfException(true)
                .waitPeriodly(sleeptime, TimeUnit.SECONDS)
                .run(() -> {
                    DescribeFreeIpaResponse freeIpaResponse = freeIpaV1Endpoint.describe(envCrn);
                    if (freeipaAvailable(freeIpaResponse)) {
                        return AttemptResults.finishWith(null);
                    } else {
                        return checkStartStatus(freeIpaResponse);
                    }
                });

    }

    private AttemptResult<Void> checkStopStatus(DescribeFreeIpaResponse freeIpaResponse) {
        if (freeIpaResponse.getStatus() == Status.STOP_FAILED) {
            LOGGER.info("Freeipa stop failed for '{}' with status {}, reason: {}", freeIpaResponse.getName(), freeIpaResponse.getStatus(), freeIpaResponse.getStatusReason());
            return AttemptResults.breakFor("Freeipa stop failed '" + freeIpaResponse.getName() + "', " + freeIpaResponse.getStatusReason());
        } else {
            return AttemptResults.justContinue();
        }
    }

    private AttemptResult<Void> checkStartStatus(DescribeFreeIpaResponse freeIpaResponse) {
        if (freeIpaResponse.getStatus() == Status.START_FAILED) {
            LOGGER.info("Freeipa start failed for '{}' with status {}, reason: {}", freeIpaResponse.getName(), freeIpaResponse.getStatus(), freeIpaResponse.getStatusReason());
            return AttemptResults.breakFor("Freeipa start failed '" + freeIpaResponse.getName() + "', " + freeIpaResponse.getStatusReason());
        } else {
            return AttemptResults.justContinue();
        }
    }

    private boolean freeipaStopped(DescribeFreeIpaResponse freeipa) {
        return freeipa.getStatus() == Status.STOPPED;
    }

    private boolean freeipaAvailable(DescribeFreeIpaResponse freeipa) {
        return freeipa.getStatus() == Status.AVAILABLE;
    }

    private boolean isStopState(Status status) {
        return Status.STOPPED.equals(status)
                || Status.STOP_IN_PROGRESS.equals(status)
                || Status.STOP_REQUESTED.equals(status)
                || Status.STOP_FAILED.equals(status);
    }

    private boolean isStartState(Status status) {
        return Status.AVAILABLE.equals(status)
                || Status.START_IN_PROGRESS.equals(status)
                || Status.START_REQUESTED.equals(status)
                || Status.START_FAILED.equals(status);
    }
}
