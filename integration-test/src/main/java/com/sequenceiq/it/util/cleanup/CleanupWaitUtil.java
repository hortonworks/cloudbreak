package com.sequenceiq.it.util.cleanup;

import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentBaseResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.environment.client.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.util.WaitResult;
import com.sequenceiq.it.cloudbreak.util.wait.PollingConfigProvider;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.client.SdxClient;

@Component
public class CleanupWaitUtil {

    private static final Logger LOG = LoggerFactory.getLogger(CleanupWaitUtil.class);

    private static final int DELETE_SLEEP = 30000;

    @Inject
    private PollingConfigProvider pollingConfigProvider;

    /**
     * Wait till all the distroxes in all the environments is going to be teminated. However not more than the "integrationtest.testsuite.maxRetry"
     *
     * Returns with:
     * FAILED: At least one of the distroxes gets in DELETE_FAILED state
     * TIMEOUT: There is at least one distrox, out of all, that is still available. However the "maxRetry" has been reached.
     * SUCCESSFUL: All the distroxes in all the environments have been terminated successfully.
     *
     * @param cloudbreak    com.sequenceiq.cloudbreak.client.CloudbreakClient
     * @param environment   com.sequenceiq.environment.client.EnvironmentClient
     * @return              FAILED, TIMEOUT, SUCCESSFUL com.sequenceiq.it.cloudbreak.util.WaitResult
     */
    public WaitResult waitForDistroxesCleanup(CloudbreakClient cloudbreak, EnvironmentClient environment) {
        int retryCount = 0;
        Map<String, String> environments = environment.environmentV1Endpoint().list().getResponses().stream()
                .collect(Collectors.toMap(response -> response.getCrn(), response -> response.getName()));

        while (retryCount < pollingConfigProvider.getMaxRetry() && checkDistroxesAreAvailable(cloudbreak, environments) && !checkDistroxesDeleteFailedStatus(cloudbreak, environments)) {
            sleep(pollingConfigProvider.getPollingInterval());
            retryCount++;
        }

        if (checkDistroxesDeleteFailedStatus(cloudbreak, environments)) {
            LOG.info("One or more DISTROX cannot be terminated in the associated environment");
            return WaitResult.FAILED;
        } else if (checkDistroxesAreAvailable(cloudbreak, environments)) {
            LOG.info("Timeout: DISTROXes cannot be terminated in environment during {} retries", pollingConfigProvider.getMaxRetry());
            return WaitResult.TIMEOUT;
        } else {
            sleep(DELETE_SLEEP);
            LOG.info("All the DISTROXs have been terminated in all the environments");
            return WaitResult.SUCCESSFUL;
        }
    }

    /**
     * Wait till all the sdxes in all the environments is going to be teminated. However not more than the "integrationtest.testsuite.maxRetry"
     *
     * Returns with:
     * FAILED: At least one of the sdxes gets in DELETE_FAILED state
     * TIMEOUT: There is at least one sdx, out of all, that is still available. However the "maxRetry" has been reached.
     * SUCCESSFUL: All the sdxes in all the environments have been terminated successfully.
     *
     * @param sdx           com.sequenceiq.sdx.client.SdxClient
     * @param environment   com.sequenceiq.environment.client.EnvironmentClient
     * @return              FAILED, TIMEOUT, SUCCESSFUL com.sequenceiq.it.cloudbreak.util.WaitResult
     */
    public WaitResult waitForSdxesCleanup(SdxClient sdx, EnvironmentClient environment) {
        int retryCount = 0;
        Map<String, String> environments = environment.environmentV1Endpoint().list().getResponses().stream()
                .collect(Collectors.toMap(response -> response.getCrn(), response -> response.getName()));

        while (retryCount < pollingConfigProvider.getMaxRetry() && checkSdxesAreAvailable(sdx, environments) && !checkSdxesDeleteFailedStatus(sdx, environments)) {
            sleep(pollingConfigProvider.getPollingInterval());
            retryCount++;
        }

        if (checkSdxesDeleteFailedStatus(sdx, environments)) {
            LOG.info("One or more SDX cannot be terminated in the associated environment");
            return WaitResult.FAILED;
        } else if (checkSdxesAreAvailable(sdx, environments)) {
            LOG.info("Timeout: SDXes cannot be terminated in environment during {} retries", pollingConfigProvider.getMaxRetry());
            return WaitResult.TIMEOUT;
        } else {
            sleep(DELETE_SLEEP);
            LOG.info("All the SDXs have been terminated in all the environments");
            return WaitResult.SUCCESSFUL;
        }
    }

    /**
     * Wait till all the environments is going to be teminated. However not more than the "integrationtest.testsuite.maxRetry"
     *
     * Returns with:
     * FAILED: At least one of the environment gets in DELETE_FAILED state
     * TIMEOUT: There is at least one environment, out of all, that is still available. However the "maxRetry" has been reached.
     * SUCCESSFUL: All the environments have been terminated successfully.
     *
     * @param environment   com.sequenceiq.environment.client.EnvironmentClient
     * @return              FAILED, TIMEOUT, SUCCESSFUL com.sequenceiq.it.cloudbreak.util.WaitResult
     */
    public WaitResult waitForEnvironmentsCleanup(EnvironmentClient environment) {
        int retryCount = 0;

        while (retryCount < pollingConfigProvider.getMaxRetry() && checkEnvironmentsAreAvailable(environment) && !checkEnvironmentsDeleteFailedStatus(environment)) {
            sleep(pollingConfigProvider.getPollingInterval());
            retryCount++;
        }

        if (checkEnvironmentsDeleteFailedStatus(environment)) {
            LOG.info("One or more environment cannot be terminated");
            return WaitResult.FAILED;
        } else if (checkEnvironmentsAreAvailable(environment)) {
            LOG.info("Timeout: Environments cannot be terminated during {} retries", pollingConfigProvider.getMaxRetry());
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

    /**
     * Checking the number of available distroxes across all the environments.
     *
     * Returns with:
     * TRUE: At least one distrox is still available in one of the environment.
     * FALSE: Distroxes cannot be found in any of the available environent.
     *
     * @param cloudbreak    com.sequenceiq.cloudbreak.client.CloudbreakClient
     * @param environments  Map of available environments CRN and Name
     * @return              TRUE or FALSE based on distroxes availability
     */
    private boolean checkDistroxesAreAvailable(CloudbreakClient cloudbreak, Map<String, String> environments) {
        return environments.entrySet().stream().anyMatch(env ->
                !cloudbreak.distroXV1Endpoint().list(env.getValue(), env.getKey()).getResponses().stream()
                        .map(StackViewV4Response::getName)
                        .collect(Collectors.toList()).isEmpty()
        );
    }

    /**
     * Checking the number of available sdxes across all the environments.
     *
     * Returns with:
     * TRUE: At least one sdx is still available in one of the environment.
     * FALSE: Sdxes cannot be found in any of the available environent.
     *
     * @param sdx           com.sequenceiq.sdx.client.SdxClient
     * @param environments  Map of available environments CRN and Name
     * @return              TRUE or FALSE based on sdxes availability
     */
    private boolean checkSdxesAreAvailable(SdxClient sdx, Map<String, String> environments) {
        return environments.entrySet().stream().anyMatch(env ->
                !sdx.sdxEndpoint().list(env.getValue()).stream()
                        .map(SdxClusterResponse::getName)
                        .collect(Collectors.toList()).isEmpty()
        );
    }

    /**
     * Checking the number of available environments.
     *
     * Returns with:
     * TRUE: At least one environment is still available.
     * FALSE: Environments cannot be found.
     *
     * @param environment   com.sequenceiq.environment.client.EnvironmentClient
     * @return              TRUE or FALSE based on environments availability
     */
    private boolean checkEnvironmentsAreAvailable(EnvironmentClient environment) {
        return !environment.environmentV1Endpoint().list().getResponses().stream()
                .map(EnvironmentBaseResponse::getName)
                .collect(Collectors.toList()).isEmpty();
    }

    /**
     * Checking DELETE_FAILED state of available distroxes across all the environments.
     *
     * Returns with:
     * TRUE: DELETE_FAILED state is available.
     * FALSE: DELETE_FAILED state cannot be found.
     *
     * @param cloudbreak    com.sequenceiq.cloudbreak.client.CloudbreakClient
     * @param environments  Map of available environments CRN and Name
     * @return              TRUE or FALSE based on existing DELETE_FAILED status
     */
    private boolean checkDistroxesDeleteFailedStatus(CloudbreakClient cloudbreak, Map<String, String> environments) {
        return environments.entrySet().stream().anyMatch(env ->
                cloudbreak.distroXV1Endpoint().list(env.getValue(), env.getKey()).getResponses().stream()
                        .anyMatch(response -> response.getStatus().equals(Status.DELETE_FAILED))
        );
    }

    /**
     * Checking DELETE_FAILED state of available sdxes across all the environments.
     *
     * Returns with:
     * TRUE: DELETE_FAILED state is available.
     * FALSE: DELETE_FAILED state cannot be found.
     *
     * @param sdx           com.sequenceiq.sdx.client.SdxClient
     * @param environments  Map of available environments CRN and Name
     * @return              TRUE or FALSE based on existing DELETE_FAILED status
     */
    private boolean checkSdxesDeleteFailedStatus(SdxClient sdx, Map<String, String> environments) {
        return environments.entrySet().stream().anyMatch(env ->
                sdx.sdxEndpoint().list(env.getValue()).stream()
                        .anyMatch(response -> response.getStatus().equals(SdxClusterStatusResponse.DELETE_FAILED))
        );
    }

    /**
     * Checking DELETE_FAILED state of available environments.
     *
     * Returns with:
     * TRUE: DELETE_FAILED state is available.
     * FALSE: DELETE_FAILED state cannot be found.
     *
     * @param environment   com.sequenceiq.environment.client.EnvironmentClient
     * @return              TRUE or FALSE based on existing DELETE_FAILED status
     */
    private boolean checkEnvironmentsDeleteFailedStatus(EnvironmentClient environment) {
        return environment.environmentV1Endpoint().list().getResponses().stream()
                .anyMatch(response -> response.getEnvironmentStatus().equals(EnvironmentStatus.DELETE_FAILED));
    }
}