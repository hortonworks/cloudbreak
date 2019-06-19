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
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxClusterStatus;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.client.EnvironmentServiceClient;

@Service
public class EnvironmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentService.class);

    private static final int SLEEP_TIME_IN_SEC_FOR_ENV_POLLING = 10;

    private static final int DURATION_IN_MINUTES_FOR_ENV_POLLING = 60;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private EnvironmentServiceClient environmentServiceClient;

    public DetailedEnvironmentResponse waitAndGetEnvironment(Long sdxId) {
        PollingConfig pollingConfig = new PollingConfig(SLEEP_TIME_IN_SEC_FOR_ENV_POLLING, TimeUnit.SECONDS,
                DURATION_IN_MINUTES_FOR_ENV_POLLING, TimeUnit.MINUTES);
        return waitAndGetEnvironment(sdxId, pollingConfig);
    }

    public DetailedEnvironmentResponse waitAndGetEnvironment(Long sdxId, PollingConfig pollingConfig) {
        Optional<SdxCluster> sdxClusterOptional = sdxClusterRepository.findById(sdxId);
        if (sdxClusterOptional.isPresent()) {
            SdxCluster sdxCluster = sdxClusterOptional.get();
            DetailedEnvironmentResponse environmentResponse = Polling.waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                    .stopIfException(false)
                    .stopAfterDelay(pollingConfig.getDuration(), pollingConfig.getDurationTimeUnit())
                    .run(() -> {
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
            sdxCluster.setStatus(SdxClusterStatus.REQUESTED);
            sdxClusterRepository.save(sdxCluster);
            return environmentResponse;
        } else {
            throw notFound("SDX cluster", sdxId).get();
        }
    }

    private DetailedEnvironmentResponse getDetailedEnvironmentResponse(String userCrn, String environmentCrn) {
        return environmentServiceClient
                .withCrn(userCrn)
                .environmentV1Endpoint()
                .getByCrn(environmentCrn);
    }

}
