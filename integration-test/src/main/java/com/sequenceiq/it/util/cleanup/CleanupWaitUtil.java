package com.sequenceiq.it.util.cleanup;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentBaseResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.environment.client.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.util.WaitResult;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
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

    /**
     * Wait till all the distroxes in all the environments is going to be teminated. However not more than the "integrationtest.testsuite.maxRetry"
     *
     * Returns with:
     * FAILED:      At least one of the distroxes gets in DELETE_FAILED state
     * TIMEOUT:     There is at least one distrox, out of all, that is still available. However the "maxRetry" has been reached.
     * SUCCESSFUL:  All the distroxes in all the environments have been terminated successfully.
     *
     * @param cloudbreak   com.sequenceiq.cloudbreak.client.CloudbreakClient
     * @param environment  com.sequenceiq.environment.client.EnvironmentClient
     * @return             FAILED, TIMEOUT, SUCCESSFUL com.sequenceiq.it.cloudbreak.util.WaitResult
     */
    public WaitResult waitForDistroxesCleanup(CloudbreakClient cloudbreak, EnvironmentClient environment) {
        int retryCount = 0;
        Map<String, String> environments = environment.environmentV1Endpoint().list(null).getResponses().stream()
                .collect(Collectors.toMap(EnvironmentBaseResponse::getCrn, EnvironmentBaseResponse::getName));

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

    /**
     * Wait till the data hub (distrox) is going to be deleted (teminated). However not more than the "integrationtest.testsuite.maxRetry"
     *
     * Returns with:
     * FAILED:      The distrox gets in DELETE_FAILED state
     * TIMEOUT:     The distrox is still available. However the "maxRetry" has been reached.
     * SUCCESSFUL:  The distrox have been terminated successfully.
     *
     * @param cloudbreakClient  com.sequenceiq.cloudbreak.client.CloudbreakClient
     * @param distroxName       Provided distrox name
     * @return                  FAILED, TIMEOUT, SUCCESSFUL com.sequenceiq.it.cloudbreak.util.WaitResult
     */
    public WaitResult waitForDistroxCleanup(CloudbreakClient cloudbreakClient, String distroxName) {
        int retryCount = 0;

        while (retryCount < maxRetry && checkDistroxIsAvailable(cloudbreakClient, distroxName)
                && !checkDistroxDeleteFailedStatus(cloudbreakClient, distroxName)) {
            sleep(pollingInterval);
            retryCount++;
        }

        if (checkDistroxDeleteFailedStatus(cloudbreakClient, distroxName)) {
            LOG.info("Failed: {} distrox cannot be terminated!", distroxName);
            return WaitResult.FAILED;
        } else if (checkDistroxIsAvailable(cloudbreakClient, distroxName)) {
            LOG.info("Timeout: {} distrox cannot be terminated during {} retries", distroxName, maxRetry);
            return WaitResult.TIMEOUT;
        } else {
            sleep(DELETE_SLEEP);
            LOG.info("Success: {} distrox have been terminated", distroxName);
            return WaitResult.SUCCESSFUL;
        }
    }

    /**
     * Wait till all the sdxes in all the environments is going to be teminated. However not more than the "integrationtest.testsuite.maxRetry"
     *
     * Returns with:
     * FAILED:      At least one of the sdxes gets in DELETE_FAILED state
     * TIMEOUT:     There is at least one sdx, out of all, that is still available. However the "maxRetry" has been reached.
     * SUCCESSFUL:  All the sdxes in all the environments have been terminated successfully.
     *
     * @param sdx          com.sequenceiq.sdx.client.SdxClient
     * @param environment  com.sequenceiq.environment.client.EnvironmentClient
     * @return             FAILED, TIMEOUT, SUCCESSFUL com.sequenceiq.it.cloudbreak.util.WaitResult
     */
    public WaitResult waitForSdxesCleanup(SdxClient sdx, EnvironmentClient environment) {
        int retryCount = 0;
        Map<String, String> environments = environment.environmentV1Endpoint().list(null).getResponses().stream()
                .collect(Collectors.toMap(EnvironmentBaseResponse::getCrn, EnvironmentBaseResponse::getName));

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

    /**
     * Wait till the data lake (sdx) is going to be deleted (terminated). However not more than the "integrationtest.testsuite.maxRetry"
     *
     * Returns with:
     * FAILED:      The sdx gets in DELETE_FAILED state
     * TIMEOUT:     The sdx is still available. However the "maxRetry" has been reached.
     * SUCCESSFUL:  The sdx have been terminated successfully.
     *
     * @param sdxClient  com.sequenceiq.sdx.client.SdxClient
     * @param sdxName    Provided sdx name
     * @return           FAILED, TIMEOUT, SUCCESSFUL com.sequenceiq.it.cloudbreak.util.WaitResult
     */
    public WaitResult waitForSdxCleanup(SdxClient sdxClient, String sdxName) {
        int retryCount = 0;

        while (retryCount < maxRetry && checkSdxIsAvailable(sdxClient, sdxName)
                && !checkSdxDeleteFailedStatus(sdxClient, sdxName)) {
            sleep(pollingInterval);
            retryCount++;
        }

        if (checkSdxDeleteFailedStatus(sdxClient, sdxName)) {
            LOG.info("Failed: {} sdx cannot be terminated", sdxName);
            return WaitResult.FAILED;
        } else if (checkSdxIsAvailable(sdxClient, sdxName)) {
            LOG.info("Timeout: {} sdx cannot be terminated during {} retries", sdxName, maxRetry);
            return WaitResult.TIMEOUT;
        } else {
            sleep(DELETE_SLEEP);
            LOG.info("Success: {} sdx have been terminated", sdxName);
            return WaitResult.SUCCESSFUL;
        }
    }

    /**
     * Wait till all the environments is going to be teminated. However not more than the "integrationtest.testsuite.maxRetry"
     *
     * Returns with:
     * FAILED:      At least one of the environment gets in DELETE_FAILED state
     * TIMEOUT:     There is at least one environment, out of all, that is still available. However the "maxRetry" has been reached.
     * SUCCESSFUL:  All the environments have been terminated successfully.
     *
     * @param environment  com.sequenceiq.environment.client.EnvironmentClient
     * @return             FAILED, TIMEOUT, SUCCESSFUL com.sequenceiq.it.cloudbreak.util.WaitResult
     */
    public WaitResult waitForEnvironmentsCleanup(EnvironmentClient environment) {
        int retryCount = 0;

        while (retryCount < maxRetry && checkEnvironmentsAreAvailable(environment) && !checkEnvironmentsDeleteFailedStatus(environment)) {
            sleep(pollingInterval);
            retryCount++;
        }

        if (checkEnvironmentsDeleteFailedStatus(environment)) {
            LOG.info("Failed: One or more environment cannot be terminated");
            return WaitResult.FAILED;
        } else if (checkEnvironmentsAreAvailable(environment)) {
            LOG.info("Timeout: Environments cannot be terminated during {} retries", maxRetry);
            return WaitResult.TIMEOUT;
        } else {
            sleep(DELETE_SLEEP);
            LOG.info("Success: All the environments have been terminated");
            return WaitResult.SUCCESSFUL;
        }
    }

    /**
     * Wait till the environment is going to be deleted (archived). However not more than the "integrationtest.testsuite.maxRetry"
     *
     * Returns with:
     * FAILED:      The environment gets in DELETE_FAILED state
     * TIMEOUT:     The environment is still available. However the "maxRetry" has been reached.
     * SUCCESSFUL:  The environments have been terminated successfully.
     *
     * @param environmentClient  com.sequenceiq.environment.client.EnvironmentClient
     * @param environmentName    Provided environment name
     * @return                   FAILED, TIMEOUT, SUCCESSFUL com.sequenceiq.it.cloudbreak.util.WaitResult
     */
    public WaitResult waitForEnvironmentCleanup(EnvironmentClient environmentClient, String environmentName) {
        int retryCount = 0;

        while (retryCount < maxRetry && checkEnvironmentIsAvailable(environmentClient, environmentName)
                && !checkEnvironmentDeleteFailedStatus(environmentClient, environmentName)) {
            sleep(pollingInterval);
            retryCount++;
        }

        if (checkEnvironmentDeleteFailedStatus(environmentClient, environmentName)) {
            LOG.info("Failed: {} environment cannot be terminated", environmentName);
            return WaitResult.FAILED;
        } else if (checkEnvironmentIsAvailable(environmentClient, environmentName)) {
            LOG.info("Timeout: {} environment cannot be terminated during {} retries", environmentName, maxRetry);
            return WaitResult.TIMEOUT;
        } else {
            sleep(DELETE_SLEEP);
            LOG.info("Success: {} environment has been terminated", environmentName);
            return WaitResult.SUCCESSFUL;
        }
    }

    private void sleep(long pollingInterval) {
        try {
            Thread.sleep(pollingInterval);
        } catch (InterruptedException ignored) {
            LOG.warn("Waiting for cleanup has been interrupted, because of: {}", ignored.getMessage(), ignored);
        }
    }

    /**
     * Checking the number of available distroxes across all the environments.
     *
     * Returns with:
     * TRUE:   At least one distrox is still available in one of the environment.
     * FALSE:  Distroxes cannot be found in any of the available environent.
     *
     * @param cloudbreak    com.sequenceiq.cloudbreak.client.CloudbreakClient
     * @param environments  Map of available environments CRN and Name
     * @return              TRUE or FALSE based on distroxes availability
     */
    private boolean checkDistroxesAreAvailable(CloudbreakClient cloudbreak, Map<String, String> environments) {
        try {
            return environments.entrySet().stream().anyMatch(env ->
                    !cloudbreak.distroXV1Endpoint().list(env.getValue(), env.getKey()).getResponses().stream()
                            .map(StackViewV4Response::getName)
                            .collect(Collectors.toList()).isEmpty()
            );
        } catch (Exception e) {
            LOG.warn("Exception has been occurred during check distroxes are available: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Checking the data hub (distrox) is still available.
     *
     * Returns with:
     * TRUE:   The distrox is still available.
     * FALSE:  The distrox cannot be found.
     *
     * @param cloudbreakClient  com.sequenceiq.cloudbreak.client.CloudbreakClient
     * @param distroxName       Provided distrox name
     * @return                  TRUE or FALSE based on distrox availability
     */
    private boolean checkDistroxIsAvailable(CloudbreakClient cloudbreakClient, String distroxName) {
        try {
            cloudbreakClient.distroXV1Endpoint().getByName(distroxName, Collections.emptySet());
            return true;
        } catch (Exception e) {
            LOG.warn("Exception has been occurred while checking {} distrox is available: {}", distroxName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Checking the number of available sdxes across all the environments.
     *
     * Returns with:
     * TRUE:   At least one sdx is still available in one of the environment.
     * FALSE:  Sdxes cannot be found in any of the available environent.
     *
     * @param sdx           com.sequenceiq.sdx.client.SdxClient
     * @param environments  Map of available environments CRN and Name
     * @return              TRUE or FALSE based on sdxes availability
     */
    private boolean checkSdxesAreAvailable(SdxClient sdx, Map<String, String> environments) {
        try {
            return environments.entrySet().stream().anyMatch(env ->
                    !(sdx.sdxEndpoint().list(env.getValue(), false).stream()
                            .map(SdxClusterResponse::getName).count() == 0)
            );
        } catch (Exception e) {
            LOG.warn("Exception has been occurred during check sdxes are available: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Checking the data lake (sdx) is still available.
     *
     * Returns with:
     * TRUE:   The sdx is still available.
     * FALSE:  The sdx cannot be found.
     *
     * @param sdxClient  com.sequenceiq.sdx.client.SdxClient
     * @param sdxName    Provided sdx name
     * @return           TRUE or FALSE based on sdx availability
     */
    private boolean checkSdxIsAvailable(SdxClient sdxClient, String sdxName) {
        try {
            sdxClient.sdxEndpoint().get(sdxName);
            return true;
        } catch (Exception e) {
            LOG.warn("Exception has been occurred while checking {} sdx is available: {}", sdxName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Checking the number of available environments.
     *
     * Returns with:
     * TRUE:   At least one environment is still available.
     * FALSE:  Environments cannot be found.
     *
     * @param environment  com.sequenceiq.environment.client.EnvironmentClient
     * @return             TRUE or FALSE based on environments availability
     */
    private boolean checkEnvironmentsAreAvailable(EnvironmentClient environment) {
        try {
            return !(environment.environmentV1Endpoint().list(null).getResponses().stream()
                    .map(EnvironmentBaseResponse::getName).count() == 0);
        } catch (Exception e) {
            LOG.warn("Exception has been occurred during check environments are available: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Checking the environment is still available.
     *
     * Returns with:
     * TRUE:   The environment is still available.
     * FALSE:  The environment cannot be found.
     *
     * @param environmentClient  com.sequenceiq.environment.client.EnvironmentClient
     * @param environmentName    Provided environment name
     * @return                   TRUE or FALSE based on environments availability
     */
    private boolean checkEnvironmentIsAvailable(EnvironmentClient environmentClient, String environmentName) {
        try {
            return environmentClient.environmentV1Endpoint().list(null).getResponses().stream()
                    .anyMatch(response -> response.getName().equalsIgnoreCase(environmentName));
        } catch (Exception e) {
            LOG.warn("Exception has been occurred while checking {} environment is available: {}", environmentName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Checking DELETE_FAILED state of available distroxes across all the environments.
     *
     * Returns with:
     * TRUE:   DELETE_FAILED state is available.
     * FALSE:  DELETE_FAILED state cannot be found.
     *
     * @param cloudbreak    com.sequenceiq.cloudbreak.client.CloudbreakClient
     * @param environments  Map of available environments CRN and Name
     * @return              TRUE or FALSE based on existing DELETE_FAILED status
     */
    private boolean checkDistroxesDeleteFailedStatus(CloudbreakClient cloudbreak, Map<String, String> environments) {
        try {
            return environments.entrySet().stream().anyMatch(env ->
                    cloudbreak.distroXV1Endpoint().list(env.getValue(), env.getKey()).getResponses().stream()
                            .anyMatch(response -> response.getStatus().equals(Status.DELETE_FAILED))
            );
        } catch (Exception e) {
            LOG.warn("Exception has been occurred during check distroxes DELETE_FAILED state: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Checking the data hub (distrox) is in DELETE_FAILED state.
     *
     * Returns with:
     * TRUE:   DELETE_FAILED state is available.
     * FALSE:  DELETE_FAILED state cannot be found.
     *
     * @param cloudbreakClient  com.sequenceiq.cloudbreak.client.CloudbreakClient
     * @param distroxName       Provided distrox name
     * @return                  TRUE or FALSE based on existing DELETE_FAILED status
     */
    private boolean checkDistroxDeleteFailedStatus(CloudbreakClient cloudbreakClient, String distroxName) {
        try {
            return cloudbreakClient.distroXV1Endpoint().getByName(distroxName, Collections.emptySet()).getStatus().equals(Status.DELETE_FAILED);
        } catch (Exception e) {
            LOG.warn("Exception has been occurred while checking {} distrox's DELETE_FAILED state: {}", distroxName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Checking DELETE_FAILED state of available sdxes across all the environments.
     *
     * Returns with:
     * TRUE:   DELETE_FAILED state is available.
     * FALSE:  DELETE_FAILED state cannot be found.
     *
     * @param sdx           com.sequenceiq.sdx.client.SdxClient
     * @param environments  Map of available environments CRN and Name
     * @return              TRUE or FALSE based on existing DELETE_FAILED status
     */
    private boolean checkSdxesDeleteFailedStatus(SdxClient sdx, Map<String, String> environments) {
        try {
            return environments.entrySet().stream().anyMatch(env ->
                    sdx.sdxEndpoint().list(env.getValue(), false).stream()
                            .anyMatch(response -> response.getStatus().equals(SdxClusterStatusResponse.DELETE_FAILED))
            );
        } catch (Exception e) {
            LOG.warn("Exception has been occurred during check sdxes DELETE_FAILED state: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Checking the data lake (sdx) is in DELETE_FAILED state.
     *
     * Returns with:
     * TRUE:   DELETE_FAILED state is available.
     * FALSE:  DELETE_FAILED state cannot be found.
     *
     * @param sdxClient  com.sequenceiq.sdx.client.SdxClient
     * @param sdxName    Provided sdx name
     * @return           TRUE or FALSE based on existing DELETE_FAILED status
     */
    private boolean checkSdxDeleteFailedStatus(SdxClient sdxClient, String sdxName) {
        try {
            return sdxClient.sdxEndpoint().get(sdxName).getStatus().equals(SdxClusterStatusResponse.DELETE_FAILED);
        } catch (Exception e) {
            LOG.warn("Exception has been occurred while checking {} sdx's DELETE_FAILED state: {}", sdxName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Checking DELETE_FAILED state of available environments.
     *
     * Returns with:
     * TRUE:   DELETE_FAILED state is available.
     * FALSE:  DELETE_FAILED state cannot be found.
     *
     * @param environment  com.sequenceiq.environment.client.EnvironmentClient
     * @return             TRUE or FALSE based on existing DELETE_FAILED status
     */
    private boolean checkEnvironmentsDeleteFailedStatus(EnvironmentClient environment) {
        try {
            return environment.environmentV1Endpoint().list(null).getResponses().stream()
                    .anyMatch(response -> response.getEnvironmentStatus().equals(EnvironmentStatus.DELETE_FAILED));
        } catch (Exception e) {
            LOG.warn("Exception has been occurred during check environments DELETE_FAILED state: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Checking the environment is in DELETE_FAILED state.
     *
     * Returns with:
     * TRUE:   DELETE_FAILED state is available.
     * FALSE:  DELETE_FAILED state is not available.
     *
     * @param environmentClient  com.sequenceiq.environment.client.EnvironmentClient
     * @param environmentName    Provided environment name
     * @return                   TRUE or FALSE based on existing DELETE_FAILED status
     */
    private boolean checkEnvironmentDeleteFailedStatus(EnvironmentClient environmentClient, String environmentName) {
        try {
            EnvironmentStatus environmentStatus = environmentClient.environmentV1Endpoint().list(null).getResponses().stream()
                    .filter(response -> response.getName().equalsIgnoreCase(environmentName))
                    .findFirst()
                    .map(EnvironmentBaseResponse::getEnvironmentStatus)
                    .orElse(EnvironmentStatus.ARCHIVED);
            LOG.info("{} environment actual state is: {}", environmentName, environmentStatus);
            return environmentStatus.equals(EnvironmentStatus.DELETE_FAILED);
        } catch (Exception e) {
            LOG.warn("Exception has been occurred while checking {} environment's DELETE_FAILED state: {}", environmentName, e.getMessage(), e);
            return false;
        }
    }
}