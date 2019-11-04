package com.sequenceiq.environment.environment.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

    private final SdxEndpoint sdxEndpoint;

    public DatalakeService(SdxEndpoint sdxEndpoint) {
        this.sdxEndpoint = sdxEndpoint;
    }

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
                                    return checkStopStatus(sdx);
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

    public void startAttachedDatalake(String envName) {
        Collection<SdxClusterResponse> attachedDatalake = getAttachedDatalake(envName);
        List<String> pollingCrn = new ArrayList<>();
        attachedDatalake.forEach(d -> {
            if (!isStartState(d.getStatus())) {
                sdxEndpoint.startByCrn(d.getCrn());
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
                                if (sdxStarted(sdx)) {
                                    return AttemptResults.<Void>finishWith(null);
                                } else {
                                    remaining.add(crn);
                                    return checkStartStatus(sdx);
                                }
                            }).collect(Collectors.toList());

                    Optional<AttemptResult<Void>> error = results.stream().filter(it -> it.getState() == AttemptState.BREAK).findFirst();
                    if (error.isPresent()) {
                        return error.get();
                    }
                    long count = results.stream().filter(it -> it.getState() == AttemptState.CONTINUE).count();
                    if (count > 0) {
                        pollingCrn.retainAll(remaining);
                        return AttemptResults.justContinue();
                    }
                    return AttemptResults.finishWith(null);
                });

    }

    private AttemptResult<Void> checkStopStatus(SdxClusterResponse sdx) {
        if (sdx.getStatus() == SdxClusterStatusResponse.STOP_FAILED) {
            LOGGER.info("SDX stop failed for '{}' with status {}, reason: {}", sdx.getName(), sdx.getStatus(), sdx.getStatusReason());
            return AttemptResults.breakFor("SDX stop failed '" + sdx.getName() + "', " + sdx.getStatusReason());
        } else {
            return AttemptResults.justContinue();
        }
    }

    private AttemptResult<Void> checkStartStatus(SdxClusterResponse sdx) {
        if (sdx.getStatus() == SdxClusterStatusResponse.START_FAILED) {
            LOGGER.info("SDX start failed for '{}' with status {}, reason: {}", sdx.getName(), sdx.getStatus(), sdx.getStatusReason());
            return AttemptResults.breakFor("SDX start failed '" + sdx.getName() + "', " + sdx.getStatusReason());
        } else {
            return AttemptResults.justContinue();
        }
    }

    private boolean sdxStopped(SdxClusterResponse sdx) {
        return sdx.getStatus() == SdxClusterStatusResponse.STOPPED;
    }

    private boolean sdxStarted(SdxClusterResponse sdx) {
        return sdx.getStatus() == SdxClusterStatusResponse.RUNNING;
    }

    private boolean isStopState(SdxClusterStatusResponse status) {
        return SdxClusterStatusResponse.STOPPED.equals(status)
                || SdxClusterStatusResponse.STOP_IN_PROGRESS.equals(status)
                || SdxClusterStatusResponse.STOP_REQUESTED.equals(status);
    }

    private boolean isStartState(SdxClusterStatusResponse status) {
        return SdxClusterStatusResponse.RUNNING.equals(status)
                || SdxClusterStatusResponse.START_IN_PROGRESS.equals(status)
                || SdxClusterStatusResponse.START_REQUESTED.equals(status)
                || SdxClusterStatusResponse.START_FAILED.equals(status);
    }
}
