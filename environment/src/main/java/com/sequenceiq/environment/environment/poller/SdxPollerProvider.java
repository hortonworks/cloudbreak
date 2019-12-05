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
import com.sequenceiq.environment.environment.service.sdx.SdxService;
import com.sequenceiq.environment.store.EnvironmentInMemoryStateStore;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@Component
public class SdxPollerProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxPollerProvider.class);

    private final SdxService sdxService;

    private final ClusterPollerResultEvaluator clusterPollerResultEvaluator;

    public SdxPollerProvider(SdxService sdxService, ClusterPollerResultEvaluator clusterPollerResultEvaluator) {
        this.sdxService = sdxService;
        this.clusterPollerResultEvaluator = clusterPollerResultEvaluator;
    }

    public AttemptMaker<Void> startSdxClustersPoller(Long envId, List<String> pollingCrns) {
        List<String> mutableCrnList = new ArrayList<>(pollingCrns);
        return () -> {
            List<String> remaining = new ArrayList<>();
            List<AttemptResult<Void>> results = collectSdxStartResults(mutableCrnList, remaining, envId);
            mutableCrnList.retainAll(remaining);
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
        SdxClusterResponse sdx = sdxService.getByCrn(sdxCrn);
        if (sdxStarted(sdx)) {
            return AttemptResults.finishWith(null);
        } else {
            remainingCrns.add(sdxCrn);
            return checkSdxStartStatus(sdx);
        }
    }

    public AttemptMaker<Void> stopSdxClustersPoller(Long envId, List<String> pollingCrns) {
        List<String> mutableCrnList = new ArrayList<>(pollingCrns);
        return () -> {
            List<String> remaining = new ArrayList<>();
            List<AttemptResult<Void>> results = collectSdxStopResults(mutableCrnList, remaining, envId);
            mutableCrnList.retainAll(remaining);
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
        SdxClusterResponse sdx = sdxService.getByCrn(crn);
        if (sdxStopped(sdx)) {
            return AttemptResults.finishWith(null);
        } else {
            remainingCrns.add(crn);
            return checkSdxStopStatus(sdx);
        }
    }

    private AttemptResult<Void> checkSdxStopStatus(SdxClusterResponse sdx) {
        if (SdxClusterStatusResponse.STOP_FAILED.equals(sdx.getStatus())) {
            LOGGER.error("SDX stop failed for '{}' with status {}, reason: {}", sdx.getName(), sdx.getStatus(), sdx.getStatusReason());
            return AttemptResults.breakFor("SDX stop failed '" + sdx.getName() + "', " + sdx.getStatusReason());
        } else {
            return AttemptResults.justContinue();
        }
    }

    private AttemptResult<Void> checkSdxStartStatus(SdxClusterResponse sdx) {
        if (SdxClusterStatusResponse.START_FAILED.equals(sdx.getStatus())) {
            LOGGER.error("SDX start failed for '{}' with status {}, reason: {}", sdx.getName(), sdx.getStatus(), sdx.getStatusReason());
            return AttemptResults.breakFor("SDX start failed '" + sdx.getName() + "', " + sdx.getStatusReason());
        } else {
            return AttemptResults.justContinue();
        }
    }

    private boolean sdxStopped(SdxClusterResponse sdx) {
        return SdxClusterStatusResponse.STOPPED.equals(sdx.getStatus());
    }

    private boolean sdxStarted(SdxClusterResponse sdx) {
        return SdxClusterStatusResponse.RUNNING.equals(sdx.getStatus());
    }

}
