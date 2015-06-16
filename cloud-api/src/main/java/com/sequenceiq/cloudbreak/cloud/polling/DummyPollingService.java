package com.sequenceiq.cloudbreak.cloud.polling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DummyPollingService extends AbstractPollingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DummyPollingService.class);

    @Value("${dummy.success.polling.count:5}")
    private int dummySuccessCount;

    @Override
    protected boolean isSuccess(PollingInfo freshPollingInfo) {
        LOGGER.debug("Determine success ...TODO");
        return freshPollingInfo.pollingCount() > dummySuccessCount;
    }

    @Override
    protected boolean isActive(PollingInfo persistedPollingInfo) {
        return persistedPollingInfo.pollingStatus().equals(PollingInfo.PollingStatus.ACTIVE);
    }

    @Override
    protected PollingInfo fetchCloudPollingInfo(PollingInfo pollingInfo) {
        LOGGER.debug("Returning the passed in pollinginfo: {}", pollingInfo);
        return pollingInfo;
    }

    @Override
    public PollingInfo handleSuccess(PollingInfo freshPollingInfo) {
        freshPollingInfo.setPollingStatus(PollingInfo.PollingStatus.SUCCESS);
        return freshPollingInfo;
    }

    @Override
    public PollingInfo handleFailure(PollingInfo freshPollingInfo) {
        // TODO interpret the failire properly (number of tries etc ...)
        freshPollingInfo.setPollingStatus(PollingInfo.PollingStatus.ACTIVE);
        return freshPollingInfo;
    }

    @Override
    public PollingInfo handleInactivePolling(PollingInfo pollingInfo) {
        pollingInfo.setPollingStatus(PollingInfo.PollingStatus.TERMINATED);
        return pollingInfo;
    }
}
