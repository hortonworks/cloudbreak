package com.sequenceiq.datalake.service.sdx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;

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

    @Value("${dl.dh.polling.attempt:360}")
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

    public void restartAttachedDistroxClustersServices(String envCrn) {
        Collection<StackViewV4Response> attachedDistroXClusters = getAttachedDistroXClusters(envCrn);

        // We allow the refresh of DHs in NODE_FAILURE states to "fix" clusters that end up in this state due to their
        // services being unable to communicate with the DL due to a migration process or some other flow.
        Collection<StackViewV4Response> availableClusters = attachedDistroXClusters.stream()
                .filter(cluster ->  {
                    if (cluster.getStatus() == Status.AVAILABLE || cluster.getStatus() == Status.NODE_FAILURE) {
                        return true;
                    } else {
                        LOGGER.info("Skipping restart for DH cluster: {}, Status: {}", cluster.getName(), cluster.getStatus().name());
                        return false;
                    }
                })
                .toList();
        ArrayList<String> pollingCrnList = availableClusters.stream().map(StackViewV4Response::getCrn).collect(Collectors.toCollection(ArrayList::new));
        if (!pollingCrnList.isEmpty()) {
            distroXV1Endpoint.restartClusterServicesByCrns(pollingCrnList, true);
            Polling.stopAfterAttempt(attempt)
                    .stopIfException(true)
                    .waitPeriodly(sleeptime, TimeUnit.SECONDS)
                    .run(checkDistroxStatus(pollingCrnList, Type.START, this::stackAndClusterStarted));
        }
    }

    public void restartDistroxServicesByCrns(List<String> crns) {
        distroXV1Endpoint.restartClusterServicesByCrns(crns, true);
    }

    public void stopAttachedDistrox(String envCrn) {
        Collection<StackViewV4Response> attachedDistroXClusters = getAttachedDistroXClusters(envCrn);
        ArrayList<String> pollingCrn = attachedDistroXClusters.stream()
                .filter(stack -> Status.STOPPED != stack.getStatus())
                .map(StackViewV4Response::getCrn)
                .collect(Collectors.toCollection(ArrayList::new));
        if (!pollingCrn.isEmpty()) {
            distroXV1Endpoint.putStopByCrns(pollingCrn);
            Polling.stopAfterAttempt(attempt)
                    .stopIfException(true)
                    .waitPeriodly(sleeptime, TimeUnit.SECONDS)
                    .run(checkDistroxStatus(pollingCrn, Type.STOP, this::stackAndClusterStopped));
        }

    }

    public void startAttachedDistrox(String envCrn) {
        Collection<StackViewV4Response> attachedDistroXClusters = getAttachedDistroXClusters(envCrn);
        ArrayList<String> pollingCrn = attachedDistroXClusters.stream().map(StackViewV4Response::getCrn).collect(Collectors.toCollection(ArrayList::new));
        if (!pollingCrn.isEmpty()) {
            distroXV1Endpoint.putStartByCrns(pollingCrn);
            Polling.stopAfterAttempt(attempt)
                    .stopIfException(true)
                    .waitPeriodly(sleeptime, TimeUnit.SECONDS)
                    .run(checkDistroxStatus(pollingCrn, Type.START, this::stackAndClusterStarted));

        }
    }

    private AttemptMaker<Void> checkDistroxStatus(ArrayList<String> pollingCrn, Type type, Function<StackV4Response, Boolean> statusChecker) {
        return () -> {
            List<String> remaining = new ArrayList<>();
            List<AttemptResult<Void>> results = pollingCrn.stream()
                    .map(crn -> {
                        StackV4Response stack = distroXV1Endpoint.getByCrn(crn, Collections.emptySet());
                        if (statusChecker.apply(stack)) {
                            return AttemptResults.<Void>finishWith(null);
                        } else {
                            remaining.add(crn);
                            return checkStatus(stack, type);
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

    private AttemptResult<Void> checkStatus(StackV4Response stack, Type type) {
        ClusterV4Response cluster = stack.getCluster();
        if (Status.STOP_FAILED.equals(stack.getStatus()) || Status.START_FAILED.equals(stack.getStatus())) {
            LOGGER.info("Datahub stack " + type +
                    " failed for '{}' with status: {} and reason: {}", stack.getName(), stack.getStatus(), stack.getStatusReason());
            return AttemptResults.breakFor("Datahub " + type + " failed '" + stack.getName() + "', " + stack.getStatusReason());
        } else if (cluster != null && (Status.STOP_FAILED.equals(cluster.getStatus()) || Status.START_FAILED.equals(cluster.getStatus()))) {
            LOGGER.info("Datahub cluster " + type +
                    " failed for '{}' status: {} and reason: {}", cluster.getName(), cluster.getStatus(), cluster.getStatusReason());
            return AttemptResults.breakFor("Datahub " + type + " failed '" + cluster.getName() + "', reason: " + cluster.getStatusReason());
        } else {
            return AttemptResults.justContinue();
        }
    }

    private boolean stackAndClusterStopped(StackV4Response stackV4Response) {
        ClusterV4Response cluster = stackV4Response.getCluster();
        return stackV4Response.getStatus().isStopped()
                && cluster != null
                && cluster.getStatus() != null
                && cluster.getStatus().isStopped();
    }

    private boolean stackAndClusterStarted(StackV4Response stackV4Response) {
        ClusterV4Response cluster = stackV4Response.getCluster();
        return stackV4Response.getStatus().isAvailable()
                && cluster != null
                && cluster.getStatus() != null
                && cluster.getStatus().isAvailable();
    }

    private enum Type {
        STOP,
        START
    }
}
