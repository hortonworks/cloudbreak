package com.sequenceiq.environment.environment.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;

@Service
public class DistroxService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroxService.class);

    @Value("${env.stop.polling.attempt:90}")
    private Integer attempt;

    @Value("${env.stop.polling.sleep.time:5}")
    private Integer sleeptime;

    private final DistroXV1Endpoint distroXV1Endpoint;

    private final PollerCollection pollerCollection;

    public DistroxService(DistroXV1Endpoint distroXV1Endpoint, PollerCollection pollerCollection) {
        this.distroXV1Endpoint = distroXV1Endpoint;
        this.pollerCollection = pollerCollection;
    }

    public Collection<StackViewV4Response> getAttachedDistroXClusters(String environmentName, String environmentCrn) {
        LOGGER.debug("Get DistroX clusters of the environment: '{}'", environmentName);
        try {
            return distroXV1Endpoint.list(environmentName, environmentCrn).getResponses();
        } catch (WebApplicationException | ProcessingException e) {
            String message = String.format("Failed to get DistroX clusters from service due to: '%s' ", e.getMessage());
            LOGGER.error(message, e);
            throw e;
        }
    }

    public void stopAttachedDistrox(Long envId, String envName) {
        Collection<StackViewV4Response> attachedDistroXClusters = getAttachedDistroXClusters(envName, null);
        List<String> pollingCrn = mapStackToCrn(attachedDistroXClusters);
        distroXV1Endpoint.putStopByCrns(pollingCrn);
        Polling.stopAfterAttempt(attempt)
                .stopIfException(true)
                .waitPeriodly(sleeptime, TimeUnit.SECONDS)
                .run(() -> {
                    List<String> remaining = new ArrayList<>();
                    List<AttemptResult<Void>> results = pollerCollection.stopDistroXPoller(pollingCrn, remaining, envId);
                    pollingCrn.retainAll(remaining);
                    return pollerCollection.evaluateResult(results);
                });
    }

    public void startAttachedDistrox(Long envId, String envName) {
        Collection<StackViewV4Response> attachedDistroXClusters = getAttachedDistroXClusters(envName, null);
        List<String> pollingCrn = mapStackToCrn(attachedDistroXClusters);
        distroXV1Endpoint.putStartByCrns(pollingCrn);
        Polling.stopAfterAttempt(attempt)
                .stopIfException(true)
                .waitPeriodly(sleeptime, TimeUnit.SECONDS)
                .run(() -> {
                    List<String> remaining = new ArrayList<>();
                    List<AttemptResult<Void>> results = pollerCollection.startDistroXPoller(pollingCrn, remaining, envId);
                    pollingCrn.retainAll(remaining);
                    return pollerCollection.evaluateResult(results);
                });

    }

    private ArrayList<String> mapStackToCrn(Collection<StackViewV4Response> attachedDistroXClusters) {
        return attachedDistroXClusters.stream().map(StackViewV4Response::getCrn).collect(Collectors.toCollection(ArrayList::new));
    }
}
