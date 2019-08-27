package com.sequenceiq.it.util.cleanup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
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

    @Value("${integrationtest.testsuite.pollingInterval:30000}")
    private long pollingInterval;

    @Value("${integrationtest.testsuite.maxRetry:1800}")
    private int maxRetry;

    public WaitResult waitForDistroxesCleanup(CloudbreakClient cloudbreak, EnvironmentClient environment) {
        WaitResult waitResult = WaitResult.SUCCESSFUL;
        Map<String, String> failedEnvironments = new HashMap<>();

        AtomicInteger retryCount = new AtomicInteger(0);
        while (checkDistroxesAreAvailable(cloudbreak, environment) && !checkDistroxesDeleteFailedStatus(cloudbreak, environment)
                && retryCount.get() < maxRetry) {
            sleep(pollingInterval);
            Map<String, String> environmentCrnNameMap = environment.environmentV1Endpoint().list().getResponses().stream()
                    .collect(Collectors.toMap(response -> response.getCrn(), response -> response.getName()));
            environmentCrnNameMap.entrySet().stream().forEach(env -> {
                LOG.info("Waiting for deleting all the DISTROXes from environment with name: {}", env.getValue());

                Map<String, Status> distroxes = cloudbreak.distroXV1Endpoint().list(env.getValue(), env.getKey()).getResponses().stream()
                        .collect(Collectors.toMap(response -> response.getName(), response -> response.getStatus()));

                distroxes.keySet().stream().forEach(distroxName -> LOG.info("Waiting for deleting, DISTROX with name: {}", distroxName));
                if (distroxes == null || distroxes.isEmpty()) {
                    LOG.info("All the DISTROXes have been deleted from environment with name: {}", env.getValue());
                    retryCount.set(maxRetry);
                }
                if (distroxes.values().stream().anyMatch(distroxStatus -> distroxStatus.equals(Status.DELETE_FAILED))) {
                    LOG.info("One or more DISTROX cannot be deleted from environment with name: {}", env.getValue());
                    failedEnvironments.put(env.getKey(), env.getValue());
                }
            });
            retryCount.getAndIncrement();
        }
        sleep(pollingInterval);

        if (!failedEnvironments.isEmpty()) {
            waitResult = WaitResult.FAILED;
            failedEnvironments.values().stream().forEach(envName -> LOG.info("One or more DISTROX cannot be deleted from environment with name: {}", envName));
        } else if (retryCount.get() >= maxRetry) {
            waitResult = WaitResult.TIMEOUT;
            failedEnvironments.values().stream().forEach(envName -> LOG.info("Timeout: {} environment cannot be cleaned up during {} retries",
                    envName, maxRetry));
        } else {
            failedEnvironments.values().stream().forEach(envName -> LOG.info("All the DISTROXs have been deleted from environment with name: {}", envName));
        }
        return waitResult;
    }

    public WaitResult waitForSdxesCleanup(SdxClient sdx, EnvironmentClient environment) {
        WaitResult waitResult = WaitResult.SUCCESSFUL;
        Map<String, String> failedEnvironments = new HashMap<>();

        AtomicInteger retryCount = new AtomicInteger(0);
        while (checkSdxesAreAvailable(sdx, environment) && !checkSdxesDeleteFailedStatus(sdx, environment) && retryCount.get() < maxRetry) {
            sleep(pollingInterval);
            Map<String, String> environmentCrnNameMap = environment.environmentV1Endpoint().list().getResponses().stream()
                    .collect(Collectors.toMap(response -> response.getCrn(), response -> response.getName()));
            environmentCrnNameMap.entrySet().stream().forEach(env -> {
                LOG.info("Waiting for deleting all the SDXes from environment with name: {}", env.getValue());

                Map<String, SdxClusterStatusResponse> sdxes = sdx.sdxEndpoint().list(env.getValue()).stream()
                        .collect(Collectors.toMap(response -> response.getName(), response -> response.getStatus()));

                sdxes.keySet().stream().forEach(sdxName -> LOG.info("Waiting for deleting, SDX with name: {}", sdxName));
                if (sdxes == null || sdxes.isEmpty()) {
                    LOG.info("All the SDXes have been deleted from environment with name: {}", env.getValue());
                    retryCount.set(maxRetry);
                }
                if (sdxes.values().stream().anyMatch(sdxStatus -> sdxStatus.equals(SdxClusterStatusResponse.DELETE_FAILED))) {
                    LOG.info("One or more SDX cannot be deleted from environment with name: {}", env.getValue());
                    failedEnvironments.put(env.getKey(), env.getValue());
                }
            });
            retryCount.getAndIncrement();
        }
        sleep(pollingInterval);

        if (!failedEnvironments.isEmpty()) {
            waitResult = WaitResult.FAILED;
            failedEnvironments.values().stream().forEach(envName -> LOG.info("One or more SDX cannot be deleted from environment with name: {}", envName));
        } else if (retryCount.get() >= maxRetry) {
            waitResult = WaitResult.TIMEOUT;
            failedEnvironments.values().stream().forEach(envName -> LOG.info("Timeout: {} environment cannot be cleaned up during {} retries",
                    envName, maxRetry));
        } else {
            failedEnvironments.values().stream().forEach(envName -> LOG.info("All the SDXes have been deleted from environment with name: {}", envName));
        }
        return waitResult;
    }

    public WaitResult waitForEnvironmentsCleanup(EnvironmentClient environment) {
        WaitResult waitResult = WaitResult.SUCCESSFUL;
        List<String> failedEnvironments = new ArrayList<>();

        int retryCount = 0;
        while (checkEnvironmentsAreAvailable(environment) && !checkEnvironmentsDeleteFailedStatus(environment) && retryCount < maxRetry) {
            sleep(pollingInterval);
            Map<String, EnvironmentStatus> environments = environment.environmentV1Endpoint().list().getResponses().stream()
                    .collect(Collectors.toMap(response -> response.getName(), response -> response.getEnvironmentStatus()));

            environments.keySet().stream().forEach(environmentName -> LOG.info("Waiting for deleting, Environment with name: {}", environmentName));

            if (environments == null || environments.isEmpty()) {
                LOG.info("All the environments have been deleted");
                retryCount = maxRetry;
            }
            if (environments.values().stream().anyMatch(environmentStatus -> environmentStatus.equals(EnvironmentStatus.DELETE_FAILED))) {
                LOG.info("One or more Environment cannot be deleted");
                failedEnvironments = environment.environmentV1Endpoint().list().getResponses().stream()
                        .filter(response -> response.getEnvironmentStatus().equals(EnvironmentStatus.DELETE_FAILED))
                        .map(response -> response.getName())
                        .collect(Collectors.toList());
            }
            retryCount++;
        }
        sleep(pollingInterval);

        if (!failedEnvironments.isEmpty()) {
            waitResult = WaitResult.FAILED;
            failedEnvironments.stream().forEach(envName -> LOG.info("One or more environment with name: {} cannot be deleted", envName));
        } else if (retryCount >= maxRetry) {
            waitResult = WaitResult.TIMEOUT;
            failedEnvironments.stream().forEach(envName -> LOG.info("Timeout: environments cannot be cleaned up during {} retries", maxRetry));
        } else {
            failedEnvironments.stream().forEach(envName -> LOG.info("All the environments have been deleted"));
        }
        return waitResult;
    }

    private void sleep(long pollingInterval) {
        try {
            Thread.sleep(pollingInterval);
        } catch (InterruptedException e) {
            LOG.warn("Ex during wait", e);
        }
    }

    private boolean checkDistroxesAreAvailable(CloudbreakClient cloudbreak, EnvironmentClient environment) {
        List<String> distroxNames = new ArrayList<>();

        Map<String, String> environmentCrnNameMap = environment.environmentV1Endpoint().list().getResponses().stream()
                .collect(Collectors.toMap(response -> response.getCrn(), response -> response.getName()));
        environmentCrnNameMap.entrySet().stream().forEach(env -> {
            LOG.info("Wait collecting available distroxes for environment: {}", env.getValue());
            distroxNames.addAll(cloudbreak.distroXV1Endpoint().list(env.getValue(), env.getKey()).getResponses().stream()
                    .map(response -> response.getName())
                    .collect(Collectors.toList()));
        });
        return !distroxNames.isEmpty();
    }

    private boolean checkSdxesAreAvailable(SdxClient sdx, EnvironmentClient environment) {
        List<String> sdxNames = new ArrayList<>();

        Map<String, String> environmentCrnNameMap = environment.environmentV1Endpoint().list().getResponses().stream()
                .collect(Collectors.toMap(response -> response.getCrn(), response -> response.getName()));
        environmentCrnNameMap.entrySet().stream().forEach(env -> {
            LOG.info("Wait collecting available sdxes for environment: {}", env.getValue());
            sdxNames.addAll(sdx.sdxEndpoint().list(env.getValue()).stream()
                    .map(response -> response.getName())
                    .collect(Collectors.toList()));
        });
        return !sdxNames.isEmpty();
    }

    private boolean checkEnvironmentsAreAvailable(EnvironmentClient environment) {
        List<String> environmentNames = environment.environmentV1Endpoint().list().getResponses().stream()
                .map(response -> response.getName())
                .collect(Collectors.toList());
        return !environmentNames.isEmpty();
    }

    private boolean checkDistroxesDeleteFailedStatus(CloudbreakClient cloudbreak, EnvironmentClient environment) {
        List<Status> distroxStatuses = new ArrayList<>();

        Map<String, String> environmentCrnNameMap = environment.environmentV1Endpoint().list().getResponses().stream()
                .collect(Collectors.toMap(response -> response.getCrn(), response -> response.getName()));
        environmentCrnNameMap.entrySet().stream().forEach(env -> {
            LOG.info("Wait collecting available distroxes' statuses for environment: {}", env.getValue());
            distroxStatuses.addAll(cloudbreak.distroXV1Endpoint().list(env.getValue(), env.getKey()).getResponses().stream()
                    .map(response -> response.getStatus())
                    .collect(Collectors.toList()));
        });
        return distroxStatuses.stream().anyMatch(status -> status.equals(Status.DELETE_FAILED));
    }

    private boolean checkSdxesDeleteFailedStatus(SdxClient sdx, EnvironmentClient environment) {
        List<SdxClusterStatusResponse> sdxStatuses = new ArrayList<>();

        Map<String, String> environmentCrnNameMap = environment.environmentV1Endpoint().list().getResponses().stream()
                .collect(Collectors.toMap(response -> response.getCrn(), response -> response.getName()));
        environmentCrnNameMap.entrySet().stream().forEach(env -> {
            LOG.info("Wait collecting available sdxes' statuses for environment: {}", env.getValue());
            sdxStatuses.addAll(sdx.sdxEndpoint().list(env.getValue()).stream()
                    .map(response -> response.getStatus())
                    .collect(Collectors.toList()));
        });
        return sdxStatuses.stream().anyMatch(status -> status.equals(SdxClusterStatusResponse.DELETE_FAILED));
    }

    private boolean checkEnvironmentsDeleteFailedStatus(EnvironmentClient environment) {
        List<EnvironmentStatus> environmentStatuses = environment.environmentV1Endpoint().list().getResponses().stream()
                .map(response -> response.getEnvironmentStatus())
                .collect(Collectors.toList());
        return environmentStatuses.stream().anyMatch(status -> status.equals(EnvironmentStatus.DELETE_FAILED));
    }
}
