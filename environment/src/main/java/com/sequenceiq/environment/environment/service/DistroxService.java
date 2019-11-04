package com.sequenceiq.environment.environment.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;

@Service
public class DistroxService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroxService.class);

    @Value("${env.stop.polling.attempt:90}")
    private Integer attempt;

    @Value("${env.stop.polling.sleep.time:5}")
    private Integer sleeptime;

    private final DistroXV1Endpoint distroXV1Endpoint;

    public DistroxService(DistroXV1Endpoint distroXV1Endpoint) {
        this.distroXV1Endpoint = distroXV1Endpoint;
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

    public void stopAttachedDistrox(String envName) {
        Collection<StackViewV4Response> attachedDistroXClusters = getAttachedDistroXClusters(envName, null);
        List<String> pollingCrn = new ArrayList<>();
        attachedDistroXClusters.forEach(d -> {
            if (!d.getStatus().isStopState()) {
                distroXV1Endpoint.putStopByCrn(d.getCrn());
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
                                StackV4Response stack = distroXV1Endpoint.getByCrn(crn, Collections.emptySet());
                                if (stackAndClusterStopped(stack, stack.getCluster())) {
                                    return AttemptResults.<Void>finishWith(null);
                                } else {
                                    remaining.add(crn);
                                    return checkStopStatus(stack);
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

    public void startAttachedDistrox(String envName) {
        Collection<StackViewV4Response> attachedDistroXClusters = getAttachedDistroXClusters(envName, null);
        List<String> pollingCrn = new ArrayList<>();
        attachedDistroXClusters.forEach(d -> {
            if (!d.getStatus().isStartState()) {
                distroXV1Endpoint.putStartByName(d.getName());
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
                                StackV4Response stack = distroXV1Endpoint.getByCrn(crn, Collections.emptySet());
                                if (stackAndClusterAvailable(stack, stack.getCluster())) {
                                    return AttemptResults.<Void>finishWith(null);
                                } else {
                                    remaining.add(crn);
                                    return checkStartStatus(stack);
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

    private AttemptResult<Void> checkStopStatus(StackV4Response stack) {
        ClusterV4Response cluster = stack.getCluster();
        if (Status.STOP_FAILED.equals(stack.getStatus())) {
            LOGGER.info("Datahub stop failed for Datahub {} with status {}, reason", stack.getName(), stack.getStatus(), stack.getStatusReason());
            return AttemptResults.breakFor("Datahub stop failed '" + stack.getName() + "', " + stack.getStatusReason());
        } else if (cluster != null && Status.STOP_FAILED.equals(cluster.getStatus())) {
            LOGGER.info("Cluster stop failed for Datahub {} status {}, reason", cluster.getName(), cluster.getStatus(), stack.getStatusReason());
            return AttemptResults.breakFor("Datahub stop failed '" + stack.getName() + "', " + stack.getStatusReason());
        } else {
            return AttemptResults.justContinue();
        }
    }

    private AttemptResult<Void> checkStartStatus(StackV4Response stack) {
        ClusterV4Response cluster = stack.getCluster();
        if (Status.START_FAILED.equals(stack.getStatus())) {
            LOGGER.info("Stack start failed for Datahub {} with status {}, reason", stack.getName(), stack.getStatus(), stack.getStatusReason());
            return AttemptResults.breakFor("Datahub start failed '" + stack.getName() + "', " + stack.getStatusReason());
        } else if (cluster != null && Status.START_FAILED.equals(cluster.getStatus())) {
            LOGGER.info("Cluster start failed for Datahub {} status {}, reason", cluster.getName(), cluster.getStatus(), stack.getStatusReason());
            return AttemptResults.breakFor("Datahub start failed '" + stack.getName() + "', " + stack.getStatusReason());
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

    private boolean stackAndClusterAvailable(StackV4Response stackV4Response, ClusterV4Response cluster) {
        return stackV4Response.getStatus().isAvailable()
                && cluster != null
                && cluster.getStatus() != null
                && cluster.getStatus().isAvailable();
    }
}
