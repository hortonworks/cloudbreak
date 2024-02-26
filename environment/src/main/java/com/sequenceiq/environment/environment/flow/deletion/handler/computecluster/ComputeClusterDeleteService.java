package com.sequenceiq.environment.environment.flow.deletion.handler.computecluster;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.core.AttemptMaker;
import com.dyngr.core.AttemptResults;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.exception.EnvironmentServiceException;
import com.sequenceiq.environment.util.PollingConfig;
import com.sequenceiq.externalizedcompute.api.endpoint.ExternalizedComputeClusterEndpoint;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterApiStatus;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterBase;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterResponse;

@Service
public class ComputeClusterDeleteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComputeClusterDeleteService.class);

    @Value("${environment.externalizedcompute.enabled}")
    private boolean externalizedComputeEnabled;

    @Inject
    private ExternalizedComputeClusterEndpoint endpoint;

    @Retryable(noRetryFor = EnvironmentServiceException.class, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public void deleteComputeCluster(String envCrn, PollingConfig pollingConfig) {
        if (externalizedComputeEnabled) {
            List<ExternalizedComputeClusterResponse> clusters = endpoint.list(envCrn);
            LOGGER.debug("Externalized clusters for the environment: {}", clusters);
            for (ExternalizedComputeClusterResponse cluster : clusters) {
                endpoint.delete(cluster.getName());
            }
            try {
                pollingDeletion(pollingConfig, () -> {
                    List<ExternalizedComputeClusterResponse> clustersUnderDeletion = endpoint.list(envCrn);
                    LOGGER.debug("Clusters under deletion: {}", clustersUnderDeletion);
                    if (!clustersUnderDeletion.isEmpty()) {
                        for (ExternalizedComputeClusterResponse clusterResponse : clustersUnderDeletion) {
                            if (ExternalizedComputeClusterApiStatus.DELETE_FAILED.equals(clusterResponse.getStatus())) {
                                return AttemptResults.breakFor("Found a cluster with delete failed status: " + clusterResponse.getName());
                            }
                        }
                        return AttemptResults.justContinue();
                    } else {
                        return AttemptResults.finishWith(null);
                    }
                });
            } catch (UserBreakException e) {
                LOGGER.warn("Externalized clusters deletion failed.", e);
                throw new EnvironmentServiceException("Externalized clusters deletion failed. Reason: " + e.getMessage());
            }
        }
    }

    public Set<String> getComputeClusterNames(EnvironmentView environment) {
        LOGGER.debug("Get Compute cluster of the environment: '{}'", environment.getName());
        if (externalizedComputeEnabled) {
            return endpoint.list(environment.getResourceCrn()).stream()
                    .map(ExternalizedComputeClusterBase::getName)
                    .collect(Collectors.toSet());
        } else {
            return Set.of();
        }
    }

    private void pollingDeletion(PollingConfig pollingConfig, AttemptMaker<Object> pollingAttempt) {
        try {
            Polling.stopAfterDelay(pollingConfig.getTimeout(), pollingConfig.getTimeoutTimeUnit())
                    .stopIfException(pollingConfig.getStopPollingIfExceptionOccured())
                    .waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                    .run(pollingAttempt);
        } catch (PollerStoppedException e) {
            LOGGER.info("Cluster deletion timed out");
            throw new EnvironmentServiceException("Cluster deletion timed out");
        }
    }

}
