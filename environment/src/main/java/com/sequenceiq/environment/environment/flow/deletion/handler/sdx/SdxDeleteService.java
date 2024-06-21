package com.sequenceiq.environment.environment.flow.deletion.handler.sdx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.Polling;
import com.dyngr.core.AttemptMaker;
import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
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
        LOGGER.debug("Calling delete for all data lakes in the environment.");

        platformAwareSdxConnector.deleteByEnvironment(environment.getResourceCrn(), force);

        LOGGER.debug("Starting poller to check all Data Lake stacks for environment {} is deleted", environment.getName());
        pollingDeletion(pollingConfig, () -> platformAwareSdxConnector.getAttemptResultForDeletion(environment.getResourceCrn()));
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
}
