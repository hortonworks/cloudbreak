package com.sequenceiq.environment.environment.flow.deletion.handler.sdx;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.util.PollingConfig;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@Component
public class SdxDeleteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxDeleteService.class);

    private final SdxEndpoint sdxEndpoint;

    public SdxDeleteService(SdxEndpoint sdxEndpoint) {
        this.sdxEndpoint = sdxEndpoint;
    }

    public void deleteSdxClustersForEnvironment(PollingConfig pollingConfig, Environment environment) {
        List<SdxClusterResponse> list = sdxEndpoint.list(environment.getName());

        LOGGER.info("Found {} Data Lake clusters for environment {}.", list.size(), environment.getName());
        if (list.isEmpty()) {
            LOGGER.info("No Data Lake clusters found for environment.");
        } else {
            waitSdxClustersDeletion(pollingConfig, environment, list);
            LOGGER.info("Data Lake deletion finished.");
        }
    }

    private void waitSdxClustersDeletion(PollingConfig pollingConfig, Environment environment, List<SdxClusterResponse> list) {
        LOGGER.debug("Calling sdxEndpoint.deleteByCrn for all data lakes [{}]",
                list.stream().map(SdxClusterResponse::getCrn).collect(Collectors.joining(", ")));

        list.forEach(s -> sdxEndpoint.deleteByCrn(s.getCrn(), true));

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
