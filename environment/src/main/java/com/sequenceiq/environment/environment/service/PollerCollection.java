package com.sequenceiq.environment.environment.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.dyngr.core.AttemptState;
import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.environment.store.EnvironmentInMemoryStateStore;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@Component
public class PollerCollection {

    private static final Logger LOGGER = LoggerFactory.getLogger(PollerCollection.class);

    private final DistroXV1Endpoint distroXV1Endpoint;

    private final SdxEndpoint sdxEndpoint;

    public PollerCollection(DistroXV1Endpoint distroXV1Endpoint, SdxEndpoint sdxEndpoint) {
        this.distroXV1Endpoint = distroXV1Endpoint;
        this.sdxEndpoint = sdxEndpoint;
    }

    public List<AttemptResult<Void>> stopDistroXPoller(List<String> pollingCrn, List<String> remaining, Long envId) {
        if (PollGroup.CANCELLED == EnvironmentInMemoryStateStore.get(envId)) {
            String message = "Datahub polling cancelled in inmemory store, id: " + envId;
            LOGGER.info(message);
            throw new PollerStoppedException(message);
        }

        return pollingCrn.stream()
                .map(crn -> fetchStopDistroXResult(remaining, crn))
                .collect(Collectors.toList());
    }

    private AttemptResult<Void> fetchStopDistroXResult(List<String> remaining, String crn) {
        StackV4Response stack = distroXV1Endpoint.getByCrn(crn, Collections.emptySet());
        if (stackAndClusterStopped(stack, stack.getCluster())) {
            return AttemptResults.finishWith(null);
        } else {
            remaining.add(crn);
            return checkStopStatus(stack);
        }
    }

    public List<AttemptResult<Void>> startDistroXPoller(List<String> pollingCrn, List<String> remaining, Long envId) {
        if (PollGroup.CANCELLED == EnvironmentInMemoryStateStore.get(envId)) {
            String message = "Datahub polling cancelled in inmemory store, id: " + envId;
            LOGGER.info(message);
            throw new PollerStoppedException(message);
        }
        return pollingCrn.stream()
                .map(crn -> fetchStartDistroXResult(remaining, crn))
                .collect(Collectors.toList());
    }

    private AttemptResult<Void> fetchStartDistroXResult(List<String> remaining, String crn) {
        StackV4Response stack = distroXV1Endpoint.getByCrn(crn, Collections.emptySet());
        if (stackAndClusterAvailable(stack, stack.getCluster())) {
            return AttemptResults.finishWith(null);
        } else {
            remaining.add(crn);
            return checkStartStatus(stack);
        }
    }

    public List<AttemptResult<Void>> stopDatalakePoller(List<String> pollingCrn, List<String> remaining, Long envId) {
        if (PollGroup.CANCELLED == EnvironmentInMemoryStateStore.get(envId)) {
            String message = "Datalake polling cancelled in inmemory store, id: " + envId;
            LOGGER.info(message);
            throw new PollerStoppedException(message);
        }
        return pollingCrn.stream()
                .map(crn -> fetchStopDatalakeResult(remaining, crn))
                .collect(Collectors.toList());
    }

    private AttemptResult<Void> fetchStopDatalakeResult(List<String> remaining, String crn) {
        SdxClusterResponse sdx = sdxEndpoint.getByCrn(crn);
        if (sdxStopped(sdx)) {
            return AttemptResults.finishWith(null);
        } else {
            remaining.add(crn);
            return checkStopStatus(sdx);
        }
    }

    public List<AttemptResult<Void>> startDatalakePoller(List<String> pollingCrn, List<String> remaining, Long envId) {
        if (PollGroup.CANCELLED == EnvironmentInMemoryStateStore.get(envId)) {
            String message = "Datalake polling cancelled in inmemory store, id: " + envId;
            LOGGER.info(message);
            throw new PollerStoppedException(message);
        }
        return pollingCrn.stream()
                .map(crn -> fetchStartDatalakeResult(crn, remaining))
                .collect(Collectors.toList());
    }

    private AttemptResult<Void> fetchStartDatalakeResult(String crn, List<String> remaining) {
        SdxClusterResponse sdx = sdxEndpoint.getByCrn(crn);
        if (sdxStarted(sdx)) {
            return AttemptResults.finishWith(null);
        } else {
            remaining.add(crn);
            return checkStartStatus(sdx);
        }
    }

    public AttemptResult<Void> evaluateResult(List<AttemptResult<Void>> results) {
        Optional<AttemptResult<Void>> error = results.stream().filter(it -> it.getState() == AttemptState.BREAK).findFirst();
        if (error.isPresent()) {
            return error.get();
        }
        long count = results.stream().filter(it -> it.getState() == AttemptState.CONTINUE).count();
        if (count > 0) {
            return AttemptResults.justContinue();
        }
        return AttemptResults.finishWith(null);
    }

    private AttemptResult<Void> checkStartStatus(StackV4Response stack) {
        ClusterV4Response cluster = stack.getCluster();
        if (Status.START_FAILED == stack.getStatus()) {
            LOGGER.info("Datahub stack start failed for '{}' with status: {} and reason: {}", stack.getName(), stack.getStatus(), stack.getStatusReason());
            return AttemptResults.breakFor("Datahub stack start failed '" + stack.getName() + "', " + stack.getStatusReason());
        } else if (cluster != null && Status.START_FAILED == cluster.getStatus()) {
            LOGGER.info("Datahub cluster start failed for '{}' with status: {} and reason: {}", cluster.getName(), cluster.getStatus(), stack.getStatusReason());
            return AttemptResults.breakFor("Datahub cluster start failed '" + cluster.getName() + "', " + cluster.getStatusReason());
        } else {
            return AttemptResults.justContinue();
        }
    }

    private boolean stackAndClusterAvailable(StackV4Response stackV4Response, ClusterV4Response cluster) {
        return stackV4Response.getStatus().isAvailable()
                && cluster != null
                && cluster.getStatus() != null
                && cluster.getStatus().isAvailable();
    }

    private AttemptResult<Void> checkStopStatus(StackV4Response stack) {
        ClusterV4Response cluster = stack.getCluster();
        if (Status.STOP_FAILED == stack.getStatus()) {
            LOGGER.info("Datahub cluster stop failed for '{}' with status: {} and reason: {}", stack.getName(), stack.getStatus(), stack.getStatusReason());
            return AttemptResults.breakFor("Datahub stack stop failed '" + stack.getName() + "', " + stack.getStatusReason());
        } else if (cluster != null && Status.STOP_FAILED == cluster.getStatus()) {
            LOGGER.info("Datahub cluster stop failed for '{}' with status: {} and reason: {}", cluster.getName(), cluster.getStatus(),
                    cluster.getStatusReason());
            return AttemptResults.breakFor("Datahub cluster stop failed '" + cluster.getName() + "', " + cluster.getStatusReason());
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

    private AttemptResult<Void> checkStopStatus(SdxClusterResponse sdx) {
        if (SdxClusterStatusResponse.STOP_FAILED == sdx.getStatus()) {
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
}
