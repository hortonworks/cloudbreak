package com.sequenceiq.freeipa.service.polling.usersync;

import javax.inject.Inject;

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
        RangerCloudIdentitySyncStatus syncStatus = sdxEndpoint.getRangerCloudIdentitySyncStatus(pollerObject.getEnvironmentCrn(), pollerObject.getCommandId());
        LOGGER.info("syncStatus = {}", syncStatus);
        switch (syncStatus.getState()) {
            case SUCCESS:
                LOGGER.info("Successfully synced cloud identity, envCrn = {}", pollerObject.getEnvironmentCrn());
                return true;
            case NOT_APPLICABLE:
                LOGGER.info("Cloud identity sync not applicable, envCrn = {}", pollerObject.getEnvironmentCrn());
                return true;
            case FAILED:
                LOGGER.error("Failed to sync cloud identity, envCrn = {}", pollerObject.getEnvironmentCrn());
                throw new CloudbreakServiceException("Failed to sync cloud identity");
            case ACTIVE:
                LOGGER.info("Sync is still in progress");
                return false;
            default:
                LOGGER.error("Encountered unknown cloud identity sync state");
                throw new CloudbreakServiceException("Failed to sync cloud identity");
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
