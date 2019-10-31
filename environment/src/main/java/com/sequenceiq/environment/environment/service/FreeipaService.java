package com.sequenceiq.environment.environment.service;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

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

    @Inject
    private FreeIpaV1Endpoint freeIpaV1Endpoint;

    public DescribeFreeIpaResponse getAttachedFreeipa(String environmentCrn) {
        LOGGER.debug("Get freeipa for the environment: '{}'", environmentCrn);
        try {
            return freeIpaV1Endpoint.describe(environmentCrn);
        } catch (WebApplicationException | ProcessingException e) {
            String message = String.format("Failed to get Freeipa from service due to: '%s' ", e.getMessage());
            LOGGER.error(message, e);
            throw e;
        }
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
                        return checkStatus(freeIpaResponse);
                    }
                });

    }

    private AttemptResult<Void> checkStatus(DescribeFreeIpaResponse freeIpaResponse) {
        if (freeIpaResponse.getStatus() == Status.STOP_FAILED) {
            LOGGER.info("Freeipa stop failed for '{}' with status {}, reason: {}", freeIpaResponse.getName(), freeIpaResponse.getStatus(), freeIpaResponse.getStatusReason());
            return sdxStopFailed(freeIpaResponse, freeIpaResponse.getStatusReason());
        } else if (!isStopState(freeIpaResponse.getStatus())) {
            return AttemptResults.breakFor("Freeipa stop failed '" + freeIpaResponse.getName() + "', stack is in inconsistency state: " + freeIpaResponse.getStatus());
        } else {
            return AttemptResults.justContinue();
        }
    }

    private AttemptResult<Void> sdxStopFailed(DescribeFreeIpaResponse freeIpaResponse, String statusReason) {
        return AttemptResults.breakFor("Freeipa stop failed '" + freeIpaResponse.getName() + "', " + statusReason);
    }

    private boolean freeipaStopped(DescribeFreeIpaResponse freeipa) {
        return freeipa.getStatus() == Status.STOPPED;
    }

    private boolean isStopState(Status status) {
        return Status.STOPPED.equals(status)
                || Status.STOP_IN_PROGRESS.equals(status)
                || Status.STOP_REQUESTED.equals(status)
                || Status.STOP_FAILED.equals(status);
    }
}
