package com.sequenceiq.environment.environment.flow.deletion.handler.sdx;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.Polling;
import com.dyngr.core.AttemptMaker;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.dyngr.exception.PollerStoppedException;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.service.EnvironmentResourceDeletionService;
import com.sequenceiq.environment.exception.SdxOperationFailedException;
import com.sequenceiq.environment.util.PollingConfig;

@Component
public class SdxDeleteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxDeleteService.class);

    private final PlatformAwareSdxConnector platformAwareSdxConnector;

    private final StackV4Endpoint stackV4Endpoint;

    private final EnvironmentResourceDeletionService environmentResourceDeletionService;

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public SdxDeleteService(PlatformAwareSdxConnector platformAwareSdxConnector,
        StackV4Endpoint stackV4Endpoint,
        EnvironmentResourceDeletionService environmentResourceDeletionService,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        this.platformAwareSdxConnector = platformAwareSdxConnector;
        this.stackV4Endpoint = stackV4Endpoint;
        this.environmentResourceDeletionService = environmentResourceDeletionService;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
    }

    public void deleteSdxClustersForEnvironment(PollingConfig pollingConfig, EnvironmentView environment, boolean force) {
        Set<String> sdxCrns = environmentResourceDeletionService.getAttachedSdxClusterCrns(environment);
        Set<String> legacySdxNames = Sets.newHashSet();
        boolean legacySdxEndpoint = false;
        // if someone use create the clusters via internal cluster API, in this case the SDX service does not know about these clusters,
        // so we need to check against legacy DL API from Core service
        if (sdxCrns.isEmpty()) {
            legacySdxNames = environmentResourceDeletionService.getDatalakeClusterNames(environment);
            legacySdxEndpoint = true;
        }

        LOGGER.info("Found {} Data Lake clusters for environment {}.", sdxCrns.size(), environment.getName());
        if (sdxCrns.isEmpty() && legacySdxNames.isEmpty()) {
            LOGGER.info("No Data Lake clusters found for environment.");
        } else if (legacySdxEndpoint) {
            waitLegacySdxClustersDeletion(pollingConfig, environment, legacySdxNames, force);
            LOGGER.info("Legacy data Lake deletion finished.");
        } else {
            waitSdxClustersDeletion(pollingConfig, environment, sdxCrns, force);
            LOGGER.info("Data Lake deletion finished.");
        }
    }

    private void waitSdxClustersDeletion(PollingConfig pollingConfig, EnvironmentView environment, Set<String> sdxCrns, boolean force) {
        LOGGER.debug("Calling sdxEndpoint.deleteByCrn for all data lakes [{}]", String.join(", ", sdxCrns));

        sdxCrns.forEach(crn -> platformAwareSdxConnector.delete(crn, force));

        LOGGER.debug("Starting poller to check all Data Lake stacks for environment {} is deleted", environment.getName());
        pollingDeletion(pollingConfig, () -> platformAwareSdxConnector.getAttemptResultForDeletion(environment.getResourceCrn(),
                environment.getName(), sdxCrns));
    }

    private void waitLegacySdxClustersDeletion(PollingConfig pollingConfig, EnvironmentView environment, Set<String> legacySdxNames, boolean force) {
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        legacySdxNames.forEach(name -> ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> stackV4Endpoint.deleteInternal(0L, name, force, initiatorUserCrn)));

        pollingDeletion(pollingConfig, () -> periodicCheckForLegacyDeletion(environment));
    }

    private void pollingDeletion(PollingConfig pollingConfig, AttemptMaker<Object> pollingAttempt) {
        try {
            Polling.stopAfterDelay(pollingConfig.getTimeout(), pollingConfig.getTimeoutTimeUnit())
                    .stopIfException(pollingConfig.getStopPollingIfExceptionOccured())
                    .waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                    .run(pollingAttempt);
        } catch (PollerStoppedException e) {
            LOGGER.info("Datalake deletion timed out");
            throw new SdxOperationFailedException("Datalake deletion timed out");
        }
    }

    private AttemptResult<Object> periodicCheckForLegacyDeletion(EnvironmentView environment) {
        StackViewV4Responses stackViewV4Responses = stackV4Endpoint.list(0L, environment.getResourceCrn(), true);
        if (!stackViewV4Responses.getResponses().isEmpty()) {
            if (stackViewV4Responses.getResponses().stream().anyMatch(c -> Status.DELETE_FAILED.equals(c.getStatus()))) {
                return AttemptResults.breakFor(new IllegalStateException("Found a cluster with delete failed status."));
            }
            return AttemptResults.justContinue();
        }
        return AttemptResults.finishWith(null);
    }
}
