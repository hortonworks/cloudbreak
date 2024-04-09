package com.sequenceiq.environment.environment.service.externalizedcompute;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.dyngr.Polling;
import com.dyngr.core.AttemptMaker;
import com.dyngr.core.AttemptResults;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.exception.EnvironmentServiceException;
import com.sequenceiq.environment.exception.ExternalizedComputeOperationFailedException;
import com.sequenceiq.environment.util.PollingConfig;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterApiStatus;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterBase;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterRequest;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterResponse;

@Component
public class ExternalizedComputeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalizedComputeService.class);

    private static final String DEFAULT_COMPUTE_CLUSTER_NAME_FORMAT = "default-%s-compute-cluster";

    @Value("${environment.externalizedCompute.enabled}")
    private boolean externalizedComputeEnabled;

    @Inject
    private ExternalizedComputeClientService externalizedComputeClientService;

    public void createComputeCluster(Environment environment) {
        try {
            if (environment.isCreateComputeCluster()) {
                if (!externalizedComputeEnabled) {
                    throw new BadRequestException("Externalized compute not enabled");
                }
                String computeClusterName = getDefaultComputeClusterName(environment.getName());
                LOGGER.info("Creating compute cluster with name {}", computeClusterName);
                ExternalizedComputeClusterRequest request = new ExternalizedComputeClusterRequest();
                request.setEnvironmentCrn(environment.getResourceCrn());
                request.setName(computeClusterName);
                externalizedComputeClientService.createComputeCluster(request);
            } else {
                LOGGER.info("Creating compute cluster is skipped because it was not configured");
            }
        } catch (ExternalizedComputeOperationFailedException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Could not create compute cluster due to:", e);
            throw new ExternalizedComputeOperationFailedException("Could not create compute cluster: " + e.getMessage(), e);
        }
    }

    public Optional<ExternalizedComputeClusterResponse> getComputeCluster(String environmentCrn, String name) {
        return externalizedComputeClientService.getComputeCluster(environmentCrn, name);
    }

    public String getDefaultComputeClusterName(String environmentName) {
        return String.format(DEFAULT_COMPUTE_CLUSTER_NAME_FORMAT, environmentName);
    }

    public void deleteComputeCluster(String envCrn, PollingConfig pollingConfig) {
        if (externalizedComputeEnabled) {
            List<ExternalizedComputeClusterResponse> clusters = externalizedComputeClientService.list(envCrn);
            LOGGER.debug("Compute clusters for the environment: {}", clusters);
            for (ExternalizedComputeClusterResponse cluster : clusters) {
                externalizedComputeClientService.deleteComputeCluster(envCrn, cluster.getName());
            }
            try {
                pollingDeletion(pollingConfig, () -> {
                    List<ExternalizedComputeClusterResponse> clustersUnderDeletion = externalizedComputeClientService.list(envCrn);
                    LOGGER.debug("Compute clusters under deletion: {}", clustersUnderDeletion);
                    if (!clustersUnderDeletion.isEmpty()) {
                        for (ExternalizedComputeClusterResponse clusterResponse : clustersUnderDeletion) {
                            if (ExternalizedComputeClusterApiStatus.DELETE_FAILED.equals(clusterResponse.getStatus())) {
                                return AttemptResults.breakFor("Found a compute cluster with delete failed status: " + clusterResponse.getName());
                            }
                        }
                        return AttemptResults.justContinue();
                    } else {
                        return AttemptResults.finishWith(null);
                    }
                });
            } catch (UserBreakException e) {
                LOGGER.warn("Compute clusters deletion failed.", e);
                throw new EnvironmentServiceException("Compute clusters deletion failed. Reason: " + e.getMessage());
            }
        } else {
            LOGGER.info("Externalized compute is disabled, skipping delete.");
        }
    }

    public Set<String> getComputeClusterNames(EnvironmentView environment) {
        LOGGER.debug("Get compute cluster names of the environment: '{}'", environment.getName());
        if (externalizedComputeEnabled) {
            return externalizedComputeClientService.list(environment.getResourceCrn()).stream()
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
            LOGGER.info("Compute cluster deletion timed out");
            throw new EnvironmentServiceException("Compute cluster deletion timed out");
        }
    }
}
