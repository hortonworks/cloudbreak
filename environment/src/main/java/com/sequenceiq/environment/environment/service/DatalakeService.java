package com.sequenceiq.environment.environment.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
import com.dyngr.core.AttemptState;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@Service
public class DatalakeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeService.class);

    @Value("${env.stop.polling.attempt:90}")
    private Integer attempt;

    @Value("${env.stop.polling.sleep.time:5}")
    private Integer sleeptime;

    @Inject
    private SdxEndpoint sdxEndpoint;

    public Collection<SdxClusterResponse> getAttachedDatalake(String environmentName) {
        LOGGER.debug("Get Datalake for the environment: '{}'", environmentName);
        try {
            return sdxEndpoint.list(environmentName);
        } catch (WebApplicationException | ProcessingException e) {
            String message = String.format("Failed to get Datalake clusters from service due to: '%s' ", e.getMessage());
            LOGGER.error(message, e);
            throw e;
        }
    }

    public void stopAttachedDatalake(String envName) {
        Collection<SdxClusterResponse> attachedDatalake = getAttachedDatalake(envName);
        List<String> pollingCrn = new ArrayList<>();
        attachedDatalake.forEach(d -> {
            if (!isStopState(d.getStatus())) {
                sdxEndpoint.stopByCrn(d.getCrn());
            }
            pollingCrn.add(d.getCrn());
        });

        List<String> remaining = new ArrayList<>();

        Polling.stopAfterAttempt(attempt)
                .stopIfException(true)
                .waitPeriodly(sleeptime, TimeUnit.SECONDS)
                .run(() -> {
                    remaining.clear();
                    List<AttemptResult<Void>> results = pollingCrn.stream()
                            .map(crn -> {
                                SdxClusterResponse sdx = sdxEndpoint.getByCrn(crn);
                                if (sdxStopped(sdx)) {
                                    return AttemptResults.<Void>finishWith(null);
                                } else {
                                    remaining.add(crn);
                                    return checkStatus(sdx);
                                }
                            }).collect(Collectors.toList());

                    Optional<AttemptResult<Void>> error = results.stream().filter(it -> it.getState() == AttemptState.BREAK).findFirst();
                    if (error.isPresent()) {
                        return error.get();
                    }
                    long count = results.stream().filter(it -> it.getState() == AttemptState.CONTINUE).count();
                    if (count > 0) {
                        return AttemptResults.justContinue();
                    }
                    pollingCrn.retainAll(remaining);
                    return AttemptResults.finishWith(null);
                });

    }

    private AttemptResult<Void> checkStatus(SdxClusterResponse sdx) {
        if (sdx.getStatus() == SdxClusterStatusResponse.STOP_FAILED) {
            LOGGER.info("SDX stop failed for '{}' with status {}, reason: {}", sdx.getName(), sdx.getStatus(), sdx.getStatusReason());
            return sdxStopFailed(sdx, sdx.getStatusReason());
        } else if (!isStopState(sdx.getStatus())) {
            return AttemptResults.breakFor("SDX stop failed '" + sdx.getName() + "', stack is in inconsistency state: " + sdx.getStatus());
        } else {
            return AttemptResults.justContinue();
        }
    }

    private AttemptResult<Void> sdxStopFailed(SdxClusterResponse sdx, String statusReason) {
        return AttemptResults.breakFor("SDX stop failed '" + sdx.getName() + "', " + statusReason);
    }

    private boolean sdxStopped(SdxClusterResponse sdx) {
        return sdx.getStatus() == SdxClusterStatusResponse.STOPPED;
    }

    private boolean isStopState(SdxClusterStatusResponse status) {
        return SdxClusterStatusResponse.STOPPED.equals(status)
                || SdxClusterStatusResponse.STOP_IN_PROGRESS.equals(status)
                || SdxClusterStatusResponse.STOP_REQUESTED.equals(status)
                || SdxClusterStatusResponse.STOP_FAILED.equals(status);
    }
}
