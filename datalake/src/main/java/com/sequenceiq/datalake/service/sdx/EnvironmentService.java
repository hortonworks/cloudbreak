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
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.statestore.DatalakeInMemoryStateStore;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.EnvironmentClientService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Service
public class EnvironmentService {

    public static final int SLEEP_TIME_IN_SEC_FOR_ENV_POLLING = 10;

    public static final int DURATION_IN_MINUTES_FOR_ENV_POLLING = 60;

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentService.class);

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private EnvironmentClientService environmentClientService;

    public DetailedEnvironmentResponse waitAndGetEnvironment(Long sdxId) {
        PollingConfig pollingConfig = new PollingConfig(SLEEP_TIME_IN_SEC_FOR_ENV_POLLING, TimeUnit.SECONDS,
                DURATION_IN_MINUTES_FOR_ENV_POLLING, TimeUnit.MINUTES);
        return waitAndGetEnvironment(sdxId, pollingConfig);
    }

    public DetailedEnvironmentResponse waitAndGetEnvironment(Long sdxId, PollingConfig pollingConfig) {
        Optional<SdxCluster> sdxClusterOptional = sdxClusterRepository.findById(sdxId);
        if (sdxClusterOptional.isPresent()) {
            SdxCluster sdxCluster = sdxClusterOptional.get();
            sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.WAIT_FOR_ENVIRONMENT, ResourceEvent.SDX_WAITING_FOR_ENVIRONMENT,
                    "Waiting for environment creation", sdxCluster);
            DetailedEnvironmentResponse environmentResponse = Polling.waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                    .stopIfException(pollingConfig.getStopPollingIfExceptionOccured())
                    .stopAfterDelay(pollingConfig.getDuration(), pollingConfig.getDurationTimeUnit())
                    .run(() -> {
                        if (PollGroup.CANCELLED.equals(DatalakeInMemoryStateStore.get(sdxCluster.getId()))) {
                            LOGGER.info("Environment wait polling cancelled in inmemory store, id: " + sdxCluster.getId());
                            return AttemptResults.breakFor("Environment wait polling cancelled in inmemory store, id: " + sdxCluster.getId());
                        }
                        LOGGER.info("Creation polling environment for environment status: '{}' in '{}' env",
                                sdxCluster.getClusterName(), sdxCluster.getEnvName());
                        DetailedEnvironmentResponse environment = getDetailedEnvironmentResponse(sdxCluster.getEnvCrn());
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
            return environmentResponse;
        } else {
            throw notFound("SDX cluster", sdxId).get();
        }
    }

    private DetailedEnvironmentResponse getDetailedEnvironmentResponse(String environmentCrn) {
        return environmentClientService.getByCrn(environmentCrn);
    }

    public DetailedEnvironmentResponse getDetailedEnvironmentResponseByName(String environmentName) {
        return environmentClientService.getByName(environmentName);
    }

}
