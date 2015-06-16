package com.sequenceiq.cloudbreak.cloud.polling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractPollingService implements PollingService<PollingInfo> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPollingService.class);

    @Override
    public PollingInfo doPoll(PollingInfo persistedPollingInfo) {
        PollingInfo freshPollingInfo = null;
        LOGGER.debug("Polling invoked with persisted polling info: {}", persistedPollingInfo);

        if (isActive(persistedPollingInfo)) {
            LOGGER.debug("The persisted polling info is active: {}", persistedPollingInfo);
            freshPollingInfo = fetchCloudPollingInfo(persistedPollingInfo);
            LOGGER.debug("The fresh polling info: {}", freshPollingInfo);

            if (isSuccess(freshPollingInfo)) {
                LOGGER.debug("Polling success; fresh polling info: {}", freshPollingInfo);
                return handleSuccess(freshPollingInfo);
            } else {
                LOGGER.debug("Polling not yet finished; fresh polling info: {}", freshPollingInfo);
                return handleFailure(freshPollingInfo);
            }
        } else {
            LOGGER.debug("The persisted polling info is not active: {}", persistedPollingInfo);
            return handleInactivePolling(persistedPollingInfo);
        }
    }

    protected abstract boolean isSuccess(PollingInfo freshPollingInfo);

    protected abstract boolean isActive(PollingInfo persistedPollingInfo);

    protected abstract PollingInfo fetchCloudPollingInfo(PollingInfo pollingInfo);

}
