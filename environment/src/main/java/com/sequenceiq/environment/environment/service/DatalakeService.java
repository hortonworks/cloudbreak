package com.sequenceiq.environment.environment.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResult;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

@Service
public class DatalakeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeService.class);

    @Value("${env.stop.polling.attempt:90}")
    private Integer attempt;

    @Value("${env.stop.polling.sleep.time:5}")
    private Integer sleeptime;

    private final SdxEndpoint sdxEndpoint;

    private final PollerCollection pollerCollection;

    public DatalakeService(SdxEndpoint sdxEndpoint, PollerCollection pollerCollection) {
        this.sdxEndpoint = sdxEndpoint;
        this.pollerCollection = pollerCollection;
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

    public void stopAttachedDatalake(Long envId, String envName) {
        Collection<SdxClusterResponse> attachedDatalake = getAttachedDatalake(envName);
        List<String> pollingCrn = new ArrayList<>();
        attachedDatalake.forEach(d -> {
            sdxEndpoint.stopByCrn(d.getCrn());
            pollingCrn.add(d.getCrn());
        });

        Polling.stopAfterAttempt(attempt)
                .stopIfException(true)
                .waitPeriodly(sleeptime, TimeUnit.SECONDS)
                .run(() -> {
                    List<String> remaining = new ArrayList<>();
                    List<AttemptResult<Void>> results = pollerCollection.stopDatalakePoller(pollingCrn, remaining, envId);
                    pollingCrn.retainAll(remaining);
                    return pollerCollection.evaluateResult(results);
                });
    }

    public void startAttachedDatalake(Long envId, String envName) {
        Collection<SdxClusterResponse> attachedDatalake = getAttachedDatalake(envName);
        List<String> pollingCrn = new ArrayList<>();
        attachedDatalake.forEach(d -> {
            sdxEndpoint.startByCrn(d.getCrn());
            pollingCrn.add(d.getCrn());
        });
        Polling.stopAfterAttempt(attempt)
                .stopIfException(true)
                .waitPeriodly(sleeptime, TimeUnit.SECONDS)
                .run(() -> {
                    List<String> remaining = new ArrayList<>();
                    List<AttemptResult<Void>> results = pollerCollection.startDatalakePoller(pollingCrn, remaining, envId);
                    pollingCrn.retainAll(remaining);
                    return pollerCollection.evaluateResult(results);
                });

    }
}
