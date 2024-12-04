package com.sequenceiq.freeipa.service.polling.usersync;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.polling.StatusCheckerTask;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.RangerCloudIdentitySyncStatus;

@Component
public class CloudIdSyncStatusListenerTask implements StatusCheckerTask<CloudIdSyncPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudIdSyncStatusListenerTask.class);

    @Inject
    private SdxEndpoint sdxEndpoint;

    @Override
    public boolean checkStatus(CloudIdSyncPollerObject pollerObject) {
        RangerCloudIdentitySyncStatus syncStatus = fetchRangerCloudIdentitySyncStatus(pollerObject);
        LOGGER.info("syncStatus = {}", syncStatus);
        return switch (syncStatus.getState()) {
            case SUCCESS -> {
                LOGGER.info("Successfully synced cloud identity, envCrn = {}", pollerObject.getEnvironmentCrn());
                yield true;
            }
            case NOT_APPLICABLE -> {
                LOGGER.info("Cloud identity sync not applicable, envCrn = {}", pollerObject.getEnvironmentCrn());
                yield true;
            }
            case FAILED -> {
                LOGGER.error("Failed to sync cloud identity, envCrn = {}", pollerObject.getEnvironmentCrn());
                throw new CloudbreakServiceException("Failed to sync cloud identity. Reason: " + syncStatus.getStatusReason());
            }
            case ACTIVE -> {
                LOGGER.info("Sync is still in progress");
                yield false;
            }
        };
    }

    private RangerCloudIdentitySyncStatus fetchRangerCloudIdentitySyncStatus(CloudIdSyncPollerObject pollerObject) {
        try {
            return sdxEndpoint.getRangerCloudIdentitySyncStatus(pollerObject.getEnvironmentCrn(), pollerObject.getCommandIds());
        } catch (Exception e) {
            LOGGER.warn("Error occurred while fetching ranger cloudidentity sync status with multiple endpoint, fallback to old endpoint for single status", e);
            return sdxEndpoint.getRangerCloudIdentitySyncStatus(pollerObject.getEnvironmentCrn(), pollerObject.getCommandIds().getFirst());
        }
    }

    @Override
    public void handleTimeout(CloudIdSyncPollerObject pollerObject) {
        String message = String.format("Operation timed out. Failed to sync cloud identity for environment = %s.", pollerObject.getEnvironmentCrn());
        throw new CloudbreakServiceException(message);
    }

    @Override
    public String successMessage(CloudIdSyncPollerObject pollerObject) {
        return String.format("Successfully synced cloud identity for envCrn = %s", pollerObject.getEnvironmentCrn());
    }

    @Override
    public boolean exitPolling(CloudIdSyncPollerObject pollerObject) {
        return false;
    }

    @Override
    public void handleException(Exception e) {
        throw new CloudbreakServiceException("Failed to sync cloud identity", e);
    }

}
