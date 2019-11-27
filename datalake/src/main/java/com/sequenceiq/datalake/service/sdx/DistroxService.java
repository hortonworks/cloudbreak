package com.sequenceiq.datalake.service.sdx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import com.dyngr.core.AttemptMaker;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.dyngr.core.AttemptState;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;

@Service
public class DistroxService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroxService.class);

    @Value("${dl.dh.polling.attempt:90}")
    private Integer attempt;

    @Value("${dl.dh.polling.sleep.time:5}")
    private Integer sleeptime;

    @Inject
    private DistroXV1Endpoint distroXV1Endpoint;

    public Collection<StackViewV4Response> getAttachedDistroXClusters(String environmentCrn) {
        LOGGER.debug("Get DistroX clusters of the environment: '{}'", environmentCrn);
        try {
            return distroXV1Endpoint.list(null, environmentCrn).getResponses();
        } catch (WebApplicationException | ProcessingException e) {
            String message = String.format("Failed to get DistroX clusters from Cloudbreak service due to: '%s' ", e.getMessage());
            LOGGER.error(message, e);
            throw e;
        }
    }

    public void stopAttachedDistrox(String envCrn) {
        Collection<StackViewV4Response> attachedDistroXClusters = getAttachedDistroXClusters(envCrn);
        ArrayList<String> pollingCrn = attachedDistroXClusters.stream().map(StackViewV4Response::getCrn).collect(Collectors.toCollection(ArrayList::new));
        distroXV1Endpoint.putStopByCrns(pollingCrn);
        Polling.stopAfterAttempt(attempt)
                .stopIfException(true)
                .waitPeriodly(sleeptime, TimeUnit.SECONDS)
                .run(checkDistroxStatus(pollingCrn));

    }

    private AttemptMaker<Void> checkDistroxStatus(ArrayList<String> pollingCrn) {
        return () -> {
            List<String> remaining = new ArrayList<>();
            List<AttemptResult<Void>> results = pollingCrn.stream()
                    .map(crn -> {
                        StackV4Response stack = distroXV1Endpoint.getByCrn(crn, Collections.emptySet());
                        if (stackAndClusterStopped(stack, stack.getCluster())) {
                            return AttemptResults.<Void>finishWith(null);
                        } else {
                            remaining.add(crn);
                            return checkStopStatus(stack);
                        }
                    }).collect(Collectors.toList());

            return evaluateResult(pollingCrn, remaining, results);
        };
    }

    private AttemptResult<Void> evaluateResult(List<String> pollingCrn, List<String> remaining, List<AttemptResult<Void>> results) {
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
    }

    private AttemptResult<Void> checkStopStatus(StackV4Response stack) {
        ClusterV4Response cluster = stack.getCluster();
        if (Status.STOP_FAILED.equals(stack.getStatus())) {
            LOGGER.info("Datahub stack stop failed for '{}' with status: {} and reason: {}", stack.getName(), stack.getStatus(), stack.getStatusReason());
            return AttemptResults.breakFor("Datahub stop failed '" + stack.getName() + "', " + stack.getStatusReason());
        } else if (cluster != null && Status.STOP_FAILED.equals(cluster.getStatus())) {
            LOGGER.info("Datahub cluster stop failed for '{}' status: {} and reason: {}", cluster.getName(), cluster.getStatus(), cluster.getStatusReason());
            return AttemptResults.breakFor("Datahub stop failed '" + cluster.getName() + "', reason: " + cluster.getStatusReason());
        } else {
            return AttemptResults.justContinue();
        }
    }

    private boolean stackAndClusterStopped(StackV4Response stackV4Response, ClusterV4Response cluster) {
        return stackV4Response.getStatus().isStopped()
                && cluster != null
                && cluster.getStatus() != null
                && cluster.getStatus().isStopped();
    }
}
