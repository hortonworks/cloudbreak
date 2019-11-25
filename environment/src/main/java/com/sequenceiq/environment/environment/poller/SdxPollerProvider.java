package com.sequenceiq.environment.environment.poller;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.core.AttemptMaker;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.environment.store.EnvironmentInMemoryStateStore;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@Component
public class SdxPollerProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxPollerProvider.class);

    private final SdxEndpoint sdxEndpoint;

    private final ClusterPollerResultEvaluator clusterPollerResultEvaluator;

    public SdxPollerProvider(SdxEndpoint sdxEndpoint, ClusterPollerResultEvaluator clusterPollerResultEvaluator) {
        this.sdxEndpoint = sdxEndpoint;
        this.clusterPollerResultEvaluator = clusterPollerResultEvaluator;
    }

    public AttemptMaker<Void> startSdxClustersPoller(Long envId, List<String> pollingCrn) {
        return () -> {
            List<String> remaining = new ArrayList<>();
            List<AttemptResult<Void>> results = collectSdxStartResults(pollingCrn, remaining, envId);
            pollingCrn.retainAll(remaining);
            return clusterPollerResultEvaluator.evaluateResult(results);
        };
    }

    private List<AttemptResult<Void>> collectSdxStartResults(List<String> pollingCrns, List<String> remainingCrns, Long envId) {
        if (PollGroup.CANCELLED.equals(EnvironmentInMemoryStateStore.get(envId))) {
            String message = "Sdx polling cancelled in inmemory store, id: " + envId;
            LOGGER.info(message);
            throw new PollerStoppedException(message);
        }
        return pollingCrns.stream()
                .map(crn -> fetchStartSdxClustersResult(crn, remainingCrns))
                .collect(Collectors.toList());
    }

    private AttemptResult<Void> fetchStartSdxClustersResult(String sdxCrn, List<String> remainingCrns) {
        SdxClusterResponse sdx = sdxEndpoint.getByCrn(sdxCrn);
        if (sdxStarted(sdx)) {
            return AttemptResults.finishWith(null);
        } else {
            remainingCrns.add(sdxCrn);
            return checkSdxStartStatus(sdx);
        }
    }

    public AttemptMaker<Void> stopSdxClustersPoller(Long envId, List<String> pollingCrn) {
        return () -> {
            List<String> remaining = new ArrayList<>();
            List<AttemptResult<Void>> results = collectSdxStopResults(pollingCrn, remaining, envId);
            pollingCrn.retainAll(remaining);
            return clusterPollerResultEvaluator.evaluateResult(results);
        };
    }

    private List<AttemptResult<Void>> collectSdxStopResults(List<String> pollingCrns, List<String> remainingCrns, Long envId) {
        if (PollGroup.CANCELLED.equals(EnvironmentInMemoryStateStore.get(envId))) {
            String message = "Sdx polling cancelled in inmemory store, id: " + envId;
            LOGGER.info(message);
            throw new PollerStoppedException(message);
        }
        return pollingCrns.stream()
                .map(crn -> fetchStopSdxClustersResult(remainingCrns, crn))
                .collect(Collectors.toList());
    }

    private AttemptResult<Void> fetchStopSdxClustersResult(List<String> remainingCrns, String crn) {
        SdxClusterResponse sdx = sdxEndpoint.getByCrn(crn);
        if (sdxStopped(sdx)) {
            return AttemptResults.finishWith(null);
        } else {
            remainingCrns.add(crn);
            return checkSdxStopStatus(sdx);
        }
    }

    private AttemptResult<Void> checkSdxStopStatus(SdxClusterResponse sdx) {
        if (sdx.getStatus() == SdxClusterStatusResponse.STOP_FAILED) {
            LOGGER.info("SDX stop failed for '{}' with status {}, reason: {}", sdx.getName(), sdx.getStatus(), sdx.getStatusReason());
            return AttemptResults.breakFor("SDX stop failed '" + sdx.getName() + "', " + sdx.getStatusReason());
        } else {
            return AttemptResults.justContinue();
        }
    }

    private AttemptResult<Void> checkSdxStartStatus(SdxClusterResponse sdx) {
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
