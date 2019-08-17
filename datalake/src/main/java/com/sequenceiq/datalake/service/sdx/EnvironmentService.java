package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.exception.NotFoundException.notFound;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.flow.statestore.DatalakeInMemoryStateStore;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.client.EnvironmentServiceCrnClient;

@Service
public class EnvironmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentService.class);

    private static final int SLEEP_TIME_IN_SEC_FOR_ENV_POLLING = 10;

    private static final int DURATION_IN_MINUTES_FOR_ENV_POLLING = 60;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private EnvironmentServiceCrnClient environmentServiceCrnClient;

    @Inject
    private SdxNotificationService notificationService;

    public DetailedEnvironmentResponse waitAndGetEnvironment(Long sdxId, String requestId) {
        PollingConfig pollingConfig = new PollingConfig(SLEEP_TIME_IN_SEC_FOR_ENV_POLLING, TimeUnit.SECONDS,
                DURATION_IN_MINUTES_FOR_ENV_POLLING, TimeUnit.MINUTES);
        return waitAndGetEnvironment(sdxId, pollingConfig, requestId);
    }

    public DetailedEnvironmentResponse waitAndGetEnvironment(Long sdxId, PollingConfig pollingConfig, String requestId) {
        Optional<SdxCluster> sdxClusterOptional = sdxClusterRepository.findById(sdxId);
        if (sdxClusterOptional.isPresent()) {
            SdxCluster sdxCluster = sdxClusterOptional.get();
            notificationService.send(ResourceEvent.SDX_WAITING_FOR_ENVIRONMENT, sdxCluster);
            sdxStatusService.setStatusForDatalake(DatalakeStatusEnum.WAIT_FOR_ENVIRONMENT,
                    "Waiting for environment creation", sdxCluster);
            DetailedEnvironmentResponse environmentResponse = Polling.waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                    .stopIfException(pollingConfig.getStopPollingIfExceptionOccured())
                    .stopAfterDelay(pollingConfig.getDuration(), pollingConfig.getDurationTimeUnit())
                    .run(() -> {
                        if (PollGroup.CANCELLED.equals(DatalakeInMemoryStateStore.get(sdxCluster.getId()))) {
                            LOGGER.info("Environment wait polling cancelled in inmemory store, id: " + sdxCluster.getId());
                            return AttemptResults.breakFor("Environment wait polling cancelled in inmemory store, id: " + sdxCluster.getId());
                        }
                        MDCBuilder.addRequestIdToMdcContext(requestId);
                        LOGGER.info("Creation polling environment for environment status: '{}' in '{}' env",
                                sdxCluster.getClusterName(), sdxCluster.getEnvName());
                        DetailedEnvironmentResponse environment = getDetailedEnvironmentResponse(
                                sdxCluster.getInitiatorUserCrn(), sdxCluster.getEnvCrn());
                        LOGGER.info("Response from environment: {}", JsonUtil.writeValueAsString(environment));
                        if (environment.getEnvironmentStatus().isAvailable()) {
                            return AttemptResults.finishWith(environment);
                        } else {
                            if (environment.getEnvironmentStatus().isFailed()) {
                                return AttemptResults.breakFor("Environment creation failed " + sdxCluster.getEnvName());
                            } else {
                                return AttemptResults.justContinue();
                            }
                        }
                    });
            sdxStatusService.setStatusForDatalake(DatalakeStatusEnum.ENVIRONMENT_CREATED, "Environment created", sdxCluster);
            sdxClusterRepository.save(sdxCluster);
            notificationService.send(ResourceEvent.SDX_ENVIRONMENT_FINISHED, sdxCluster);
            return environmentResponse;
        } else {
            throw notFound("SDX cluster", sdxId).get();
        }
    }

    private DetailedEnvironmentResponse getDetailedEnvironmentResponse(String userCrn, String environmentCrn) {
        return environmentServiceCrnClient
                .withCrn(userCrn)
                .environmentV1Endpoint()
                .getByCrn(environmentCrn);
    }

}
