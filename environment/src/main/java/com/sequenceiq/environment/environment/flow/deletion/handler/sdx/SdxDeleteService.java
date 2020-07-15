package com.sequenceiq.environment.environment.flow.deletion.handler.sdx;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.service.EnvironmentResourceDeletionService;
import com.sequenceiq.environment.util.PollingConfig;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@Component
public class SdxDeleteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxDeleteService.class);

    private final SdxEndpoint sdxEndpoint;

    private final StackV4Endpoint stackV4Endpoint;

    private final EnvironmentResourceDeletionService environmentResourceDeletionService;

    public SdxDeleteService(SdxEndpoint sdxEndpoint, StackV4Endpoint stackV4Endpoint, EnvironmentResourceDeletionService environmentResourceDeletionService) {
        this.sdxEndpoint = sdxEndpoint;
        this.stackV4Endpoint = stackV4Endpoint;
        this.environmentResourceDeletionService = environmentResourceDeletionService;
    }

    public void deleteSdxClustersForEnvironment(PollingConfig pollingConfig, Environment environment, boolean force) {
        Set<String> sdxCrnsOrDatalakeName = environmentResourceDeletionService.getAttachedSdxClusterCrns(environment);
        boolean legacySdxEndpoint = false;
        // if someone use create the clusters via internal cluster API, in this case the SDX service does not know about these clusters,
        // so we need to check against legacy DL API from Core service
        if (sdxCrnsOrDatalakeName.isEmpty()) {
            sdxCrnsOrDatalakeName = environmentResourceDeletionService.getDatalakeClusterNames(environment);
            legacySdxEndpoint = true;
        }

        LOGGER.info("Found {} Data Lake clusters for environment {}.", sdxCrnsOrDatalakeName.size(), environment.getName());
        if (sdxCrnsOrDatalakeName.isEmpty()) {
            LOGGER.info("No Data Lake clusters found for environment.");
        } else {
            waitSdxClustersDeletion(pollingConfig, environment, sdxCrnsOrDatalakeName, legacySdxEndpoint, force);
            LOGGER.info("Data Lake deletion finished.");
        }
    }

    private void waitSdxClustersDeletion(PollingConfig pollingConfig, Environment environment, Set<String> sdxCrnsOrDatalakeName,
        boolean legacySdxEndpoint, boolean force) {
        LOGGER.debug("Calling sdxEndpoint.deleteByCrn for all data lakes [{}]", String.join(", ", sdxCrnsOrDatalakeName));

        if (legacySdxEndpoint) {
            sdxCrnsOrDatalakeName.forEach(name -> stackV4Endpoint.delete(0L, name, force,
                    Crn.fromString(environment.getResourceCrn()).getAccountId()));
        } else {
            sdxCrnsOrDatalakeName.forEach(crn -> sdxEndpoint.deleteByCrn(crn, force));
        }

        LOGGER.debug("Starting poller to check all Data Lake stacks for environment {} is deleted", environment.getName());
        Polling.stopAfterDelay(pollingConfig.getTimeout(), pollingConfig.getTimeoutTimeUnit())
                .stopIfException(pollingConfig.getStopPollingIfExceptionOccured())
                .waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                .run(() -> periodicCheckForDeletion(environment));
    }

    private AttemptResult<Object> periodicCheckForDeletion(Environment environment) {
        List<SdxClusterResponse> actualClusterList = sdxEndpoint.list(environment.getName());
        if (!actualClusterList.isEmpty()) {
            if (actualClusterList.stream().anyMatch(c -> c.getStatus() == SdxClusterStatusResponse.DELETE_FAILED)) {
                return AttemptResults.breakFor(new IllegalStateException("Found a cluster with delete failed status."));
            }
            return AttemptResults.justContinue();
        }
        return AttemptResults.finishWith(null);
    }
}
