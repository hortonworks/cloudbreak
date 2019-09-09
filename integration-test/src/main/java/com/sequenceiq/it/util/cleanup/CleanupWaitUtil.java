package com.sequenceiq.it.util.cleanup;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.environment.client.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.util.WaitResult;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.client.SdxClient;

@Component
public class CleanupWaitUtil {

    private static final Logger LOG = LoggerFactory.getLogger(CleanupWaitUtil.class);

    private static final int DELETE_SLEEP = 30000;

    @Value("${integrationtest.testsuite.pollingInterval:30000}")
    private long pollingInterval;

    @Value("${integrationtest.testsuite.maxRetry:3000}")
    private int maxRetry;

    public WaitResult waitForDistroxesCleanup(CloudbreakClient cloudbreak, EnvironmentClient environment) {
        int retryCount = 0;
        Map<String, String> environments = environment.environmentV1Endpoint().list().getResponses().stream()
                .collect(Collectors.toMap(response -> response.getCrn(), response -> response.getName()));

        while (retryCount < maxRetry && checkDistroxesAreAvailable(cloudbreak, environments) && !checkDistroxesDeleteFailedStatus(cloudbreak, environments)) {
            sleep(pollingInterval);
            retryCount++;
        }

        if (checkDistroxesDeleteFailedStatus(cloudbreak, environments)) {
            LOG.info("One or more DISTROX cannot be terminated in the associated environment");
            return WaitResult.FAILED;
        } else if (checkDistroxesAreAvailable(cloudbreak, environments)) {
            LOG.info("Timeout: DISTROXes cannot be terminated in environment during {} retries", maxRetry);
            return WaitResult.TIMEOUT;
        } else {
            sleep(DELETE_SLEEP);
            LOG.info("All the DISTROXs have been terminated in all the environments");
            return WaitResult.SUCCESSFUL;
        }
    }

    public WaitResult waitForSdxesCleanup(SdxClient sdx, EnvironmentClient environment) {
        int retryCount = 0;
        Map<String, String> environments = environment.environmentV1Endpoint().list().getResponses().stream()
                .collect(Collectors.toMap(response -> response.getCrn(), response -> response.getName()));

        while (retryCount < maxRetry && checkSdxesAreAvailable(sdx, environments) && !checkSdxesDeleteFailedStatus(sdx, environments)) {
            sleep(pollingInterval);
            retryCount++;
        }

        if (checkSdxesDeleteFailedStatus(sdx, environments)) {
            LOG.info("One or more SDX cannot be terminated in the associated environment");
            return WaitResult.FAILED;
        } else if (checkSdxesAreAvailable(sdx, environments)) {
            LOG.info("Timeout: SDXes cannot be terminated in environment during {} retries", maxRetry);
            return WaitResult.TIMEOUT;
        } else {
            sleep(DELETE_SLEEP);
            LOG.info("All the SDXs have been terminated in all the environments");
            return WaitResult.SUCCESSFUL;
        }
    }

    public WaitResult waitForEnvironmentsCleanup(EnvironmentClient environment) {
        int retryCount = 0;

        while (retryCount < maxRetry && checkEnvironmentsAreAvailable(environment) && !checkEnvironmentsDeleteFailedStatus(environment)) {
            sleep(pollingInterval);
            retryCount++;
        }

        if (checkEnvironmentsDeleteFailedStatus(environment)) {
            LOG.info("One or more environment cannot be terminated");
            return WaitResult.FAILED;
        } else if (checkEnvironmentsAreAvailable(environment)) {
            LOG.info("Timeout: Environments cannot be terminated during {} retries", maxRetry);
            return WaitResult.TIMEOUT;
        } else {
            sleep(DELETE_SLEEP);
            LOG.info("All the environments have been terminated");
            return WaitResult.SUCCESSFUL;
        }
    }

    private void sleep(long pollingInterval) {
        try {
            Thread.sleep(pollingInterval);
        } catch (InterruptedException e) {
            LOG.warn("Exception has been occured during wait: ", e);
        }
    }

    private boolean checkDistroxesAreAvailable(CloudbreakClient cloudbreak, Map<String, String> environments) {
        AtomicBoolean distroxesAreAvailable = new AtomicBoolean(true);

        environments.entrySet().stream().forEach(env -> {
            Map<String, Status> distroxes = cloudbreak.distroXV1Endpoint().list(env.getValue(), env.getKey()).getResponses().stream()
                    .collect(Collectors.toMap(response -> response.getName(), response -> response.getStatus()));

            if (distroxes == null || distroxes.isEmpty()) {
                LOG.info("All the DISTROXes have been deleted from environment with name: {}", env.getValue());
                distroxesAreAvailable.set(false);
            }
        });
        return distroxesAreAvailable.get();
    }

    private boolean checkSdxesAreAvailable(SdxClient sdx, Map<String, String> environments) {
        AtomicBoolean sdxesAreAvailable = new AtomicBoolean(true);

        environments.entrySet().stream().forEach(env -> {
            Map<String, SdxClusterStatusResponse> sdxes = sdx.sdxEndpoint().list(env.getValue()).stream()
                    .collect(Collectors.toMap(response -> response.getName(), response -> response.getStatus()));

            if (sdxes == null || sdxes.isEmpty()) {
                LOG.info("All the SDXes have been deleted from environment with name: {}", env.getValue());
                sdxesAreAvailable.set(false);
            }
        });
        return sdxesAreAvailable.get();
    }

    private boolean checkEnvironmentsAreAvailable(EnvironmentClient environment) {
        Map<String, EnvironmentStatus> environments = environment.environmentV1Endpoint().list().getResponses().stream()
                .collect(Collectors.toMap(response -> response.getName(), response -> response.getEnvironmentStatus()));

        return (environments == null || environments.isEmpty()) ? false : true;
    }

    private boolean checkDistroxesDeleteFailedStatus(CloudbreakClient cloudbreak, Map<String, String> environments) {
        AtomicBoolean failedIsAvailable = new AtomicBoolean(false);

        environments.entrySet().stream().forEach(env -> {
            Map<String, Status> distroxes = cloudbreak.distroXV1Endpoint().list(env.getValue(), env.getKey()).getResponses().stream()
                    .collect(Collectors.toMap(response -> response.getName(), response -> response.getStatus()));

            if (distroxes.values().stream().anyMatch(distroxStatus -> distroxStatus.equals(Status.DELETE_FAILED))) {
                LOG.info("One or more DISTROX cannot be deleted from environment with name: {}", env.getValue());
                failedIsAvailable.set(true);
            }
        });
        return failedIsAvailable.get();
    }

    private boolean checkSdxesDeleteFailedStatus(SdxClient sdx, Map<String, String> environments) {
        AtomicBoolean failedIsAvailable = new AtomicBoolean(false);

        environments.entrySet().stream().forEach(env -> {
            Map<String, SdxClusterStatusResponse> sdxes = sdx.sdxEndpoint().list(env.getValue()).stream()
                    .collect(Collectors.toMap(response -> response.getName(), response -> response.getStatus()));

            if (sdxes.values().stream().anyMatch(sdxStatus -> sdxStatus.equals(SdxClusterStatusResponse.DELETE_FAILED))) {
                LOG.info("One or more SDX cannot be deleted from environment with name: {}", env.getValue());
                failedIsAvailable.set(true);
            }
        });
        return failedIsAvailable.get();
    }

    private boolean checkEnvironmentsDeleteFailedStatus(EnvironmentClient environment) {
        Map<String, EnvironmentStatus> environments = environment.environmentV1Endpoint().list().getResponses().stream()
                .collect(Collectors.toMap(response -> response.getName(), response -> response.getEnvironmentStatus()));

        return environments.values().stream().anyMatch(environmentStatus -> environmentStatus.equals(EnvironmentStatus.DELETE_FAILED)) ? true : false;
    }
}