package com.sequenceiq.environment.environment.flow.deletion.handler.datahub;

import java.util.Collection;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.DistroXMultiDeleteV1Request;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.service.datahub.DatahubService;
import com.sequenceiq.environment.exception.DatahubOperationFailedException;
import com.sequenceiq.environment.util.PollingConfig;

@Component
public class DatahubDeletionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatahubDeletionService.class);

    private final DatahubService datahubService;

    public DatahubDeletionService(DatahubService datahubService) {
        this.datahubService = datahubService;
    }

    public void deleteDatahubClustersForEnvironment(PollingConfig pollingConfig, Environment environment, boolean force) {
        Collection<StackViewV4Response> list = datahubService.list(environment.getResourceCrn()).getResponses();
        LOGGER.info("Found {} Data Hub clusters for environment {}.", list.size(), environment.getName());
        if (list.isEmpty()) {
            LOGGER.info("No Data Hub clusters found for environment.");
        } else {
            waitDatahubClustersDeletion(pollingConfig, environment, list, force);
            LOGGER.info("Data hub deletion finished.");
        }
    }

    private void waitDatahubClustersDeletion(PollingConfig pollingConfig, Environment environment, Collection<StackViewV4Response> list,
        boolean force) {
        DistroXMultiDeleteV1Request multiDeleteRequest = new DistroXMultiDeleteV1Request();
        multiDeleteRequest.setCrns(list.stream().map(StackViewV4Response::getCrn).collect(Collectors.toSet()));
        LOGGER.debug("Calling distroXV1Endpoint.deleteMultiple with crn [{}]", multiDeleteRequest.getCrns());
        datahubService.deleteMultiple(environment.getResourceCrn(), multiDeleteRequest, force);

        LOGGER.debug("Starting poller to check all Datahub stacks for environment {} are deleted", environment.getName());
        try {
            Polling.stopAfterDelay(pollingConfig.getTimeout(), pollingConfig.getTimeoutTimeUnit())
                    .stopIfException(pollingConfig.getStopPollingIfExceptionOccured())
                    .waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                    .run(() -> periodicCheckForDeletion(environment));
        } catch (PollerStoppedException e) {
            LOGGER.info("Datahub cluster deletion timed out");
            throw new DatahubOperationFailedException("Datahub cluster deletion timed out", e);
        }
    }

    private AttemptResult<Object> periodicCheckForDeletion(Environment environment) {
        Collection<StackViewV4Response> actualClusterList = datahubService.list(environment.getResourceCrn()).getResponses();
        if (!actualClusterList.isEmpty()) {
            if (actualClusterList.stream().anyMatch(c -> c.getStatus() == Status.DELETE_FAILED)) {
                return AttemptResults.breakFor(new IllegalStateException("Found a cluster with delete failed status."));
            }
            return AttemptResults.justContinue();
        }
        return AttemptResults.finishWith(null);
    }
}
